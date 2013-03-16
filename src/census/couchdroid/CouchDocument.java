/*
Copyright 2007 Fourspaces Consulting, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package census.couchdroid;

import android.annotation.TargetApi;
import android.os.Build;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

//import com.fourspaces.couchdb.util.JSONUtils;
import org.json.*;

//import org.apache.commons.lang.StringUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;


/**
* Everything in CouchDB is a Document.  In this case, the document is an object backed by a 
* JSONObject.  The Document is also aware of the database that it is connected to.  This allows
* the Document to reload it's properties when needed.  The only special fields are "_id", "_rev", 
* "_revisions", and "_view_*".
* <p>
* All document have an _id and a _rev.  If this is a new document those fields are populated when
* they are saved to the CouchDB server.
* <p>
* _revisions is only populated if the document has been retrieved via database.getDocumentWithRevisions();
* So, if this document wasn't, then when you call document.getRevisions(), it will go back to the server
* to reload itself via database.getDocumentWithRevisions().
* <p>
* The Document can be treated like a JSONObject, eventhough it doesn't extend JSONObject (it's final).
* <p>
* You can also get/set values by calling document.get(key), document.put(key,value), just like a Map.
* <p>
* You can get a handle on the backing JSONObject by calling document.getJSONObject();  If this hasn't
* been loaded yet, it will load the data itself using the given database connection.
* <p>
* If you got this Document from a view, you are likely missing elements.  To load them you can call
* document.load().
* 
* @author mbreese
*
*/
@SuppressWarnings("unchecked")
public class CouchDocument implements Map {
 //Log log = LogFactory.getLog(Document.class);

public static final String REVISION_HISTORY_PROP = "_revisions";

 protected CouchDatabase database = null;
 protected JSONObject object;
 
 boolean loaded = false;
 
 /**
  * Create a new Document
  *
  */
 public CouchDocument () {
     this.object = new JSONObject();
 }
 /**
  * Create a new Document from a JSONObject
  * @param obj
  */
 public CouchDocument (JSONObject obj) {
     this.object = obj;
     loaded=true;
 }

/**
  * Load data into this document from a differing JSONObject 
  * <p>
  * This is mainly for reloading data for an object that was retrieved from a view.  This version
  * doesn't overwrite any unsaved data that is currently present in this object.
  * 
  * @param object2
  */ 
 protected void load(JSONObject object2) {
     if (!loaded) {
         
         // this is sad. Is there no copy constructor for JSON objects???
         try {
            object = new JSONObject(object2.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
         loaded=true;
     }
 }
 
 /**
  * This document's id (if saved)
  * @return
  */
 public String getId() {
   if (!object.optString("_id").trim().isEmpty()) {
     return object.optString("_id");
   } else {  
     return object.optString("id");
   }
 }
 public void setId(String id)  {
     try {
        object.put("_id",id);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
 }

 /**
  * This strips _design from the document id
  */
 public String getViewDocumentId() {
     String id = getId();
     int pos = id.lastIndexOf("/");
     if (pos == -1) {
         return id;
     } else {
         return id.substring(pos+1);
     }
 }

 /**
  * This document's Revision (if saved)
  * @return
  */
 public String getRev()  {
 if (!object.optString("_rev").trim().isEmpty()) {
   return object.optString("_rev");
 } else {
   return object.optString("rev");
 }
 }
 public void setRev(String rev)  {
     try {
        object.put("_rev", rev);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
 }
 
 /**
  * A list of the revision numbers that this document has.  If this hasn't been 
  * populated with a "full=true" query, then the database will be re-queried
  * @return
  */
 public String[] getRevisions() throws IOException {
     String[] revs = null;
     if (!object.has("_revs")) {
         populateRevisions();
     } 
     //System.out.println(object);
     JSONArray ar = null;
    try {
        ar = object.getJSONObject(REVISION_HISTORY_PROP).getJSONArray("ids");
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     if (ar!=null) {
         revs = new String[ar.length()];
         for (int i=0 ; i< ar.length(); i++) {
             try {
                revs[i]=ar.getString(i);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
         }
     }
     return revs;
 }
 
 /**
  * Get a named view that is stored in the document.
  * @param name
  * @return
  */
 public CouchView getView(String name) {
     if (object.has("views")) {
         JSONObject views = null;
        try {
            views = object.getJSONObject("views");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
         if (views.has(name)) {
             return new CouchView(this,name);
         }
     }
     return null;
 }
 
 /**
  * Add a view to this document.  If a view function already exists with the given viewName
  * it is overwritten.
  * <p>
  * This isn't persisted until the document is saved.
  * 
  * @param designDoc document name
  * @param viewName
  * @param function
  * @return
 * @throws JSONException 
  */
 public CouchView addView(String designDoc, String viewName, String function) throws JSONException {
     object.put("_id", "_design/"+ designDoc); //Not sure if _id or id should be used
     object.put("language", "javascript"); //FIXME specify language

 JSONObject funcs = new JSONObject();
// System.err.println("JSON String: " + JSONUtils.stringSerializedFunction(function));
// funcs.put("map", JSONUtils.stringSerializedFunction(function));
 funcs.accumulate("map", "\"" + function + "\"");

 System.err.println("FUNCS: " + funcs.toString());

     JSONObject viewMap = new JSONObject();
     viewMap.put(viewName, funcs);

     object.put("views", viewMap);

     return new CouchView(this, viewName, function);

 }
 
 /**
  * Adds an update handler to the document and sets the document ID to that of a design 
  * document. If the function name key already exists within the "updates" element it will 
  * be overwritten. The document must be saved for the change to persist. 
  * @author rwilson
  * @param designDoc
  * @param functionName
  * @param function
 * @throws JSONException 
  */
 public void addUpdateHandler(String designDoc, String functionName, String function) throws JSONException {
   object.put("_id", "_design/"+ designDoc); 
 
   if (object.has("updates")) {
   JSONObject updates = object.getJSONObject("updates");
   updates.put(functionName, "\"" + function + "\"");
 } else {
   JSONObject func = new JSONObject();
   func.put(functionName, "\"" + function + "\"");
   object.put("updates", func);
 }
 }
 
 /**
  * Adds an update handler to the document. The ID of the document is not modified. If the function 
  * name key already exists within the "updates" element it will be overwritten. The document 
  * must be saved for the change to persist.
  * @author rwilson
  * @param functionName
  * @param function
  * @return
 * @throws JSONException 
  */
 public void addUpdateHandler(String functionName, String function) throws JSONException {
   if (object.has("updates")) {
     JSONObject updates = object.getJSONObject("updates");
     updates.put(functionName, "\"" + function + "\"");
   } else {
     JSONObject func = new JSONObject();
     func.put(functionName, "\"" + function + "\"");
     object.put("updates", func);
   }
 }   
 
 /**
  * Removes a view from this document.
  * <p>
  * This isn't persisted until the document is saved.
  * @param viewName
  */
 public void deleteView(String viewName) {
     object.remove("_design/"+viewName);
 }
 
 void setDatabase(CouchDatabase database) {
     this.database=database;
 }
 
 /**
  * Loads data from the server for this document.  Actually requests a new copy of data from the 
  * server and uses that to populate this document.  This doesn't overwrite any unsaved data.
  */
 public void refresh() throws IOException {
     if (database!=null) {
         CouchDocument doc = database.getDocument(getId());
         //log.info("Loading: "+doc.getJSONObject());
         load(doc.getJSONObject());
     }
 }
 
 protected void populateRevisions() throws IOException {
     if (database!=null) {
         CouchDocument doc = database.getDocumentWithRevisions(getId());
         //log.info("Loading: "+doc.getJSONObject());
         load(doc.getJSONObject());
     }
 }
 
 /**
  * Retrieves the backing JSONObject
  * @return
  */
 public JSONObject getJSONObject() {
     if (!loaded && database!=null && getId()!=null && !getId().equals("")) {
         try {
     refresh();
   } catch (IOException e) {
     throw new RuntimeException("error in refreshing Document", e);
   }
     }
     return object;
 }
 
 public String toString() {
     return object.toString();
 }
 
 /*
  * Delegate methods to the JSON Object.
  */ 
 public JSONObject accumulate(String arg0, boolean arg1) {
     try {
        return getJSONObject().accumulate(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return null;
 }
 public JSONObject accumulate(String arg0, double arg1) {
     try {
        return getJSONObject().accumulate(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return null;
 }
 public JSONObject accumulate(String arg0, int arg1) {
     try {
        return getJSONObject().accumulate(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return null;
 }
 public JSONObject accumulate(String arg0, long arg1) {
     try {
        return getJSONObject().accumulate(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return null;
 }
 public JSONObject accumulate(String arg0, Object arg1) {
     try {
        return getJSONObject().accumulate(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return null;
 }
 @SuppressWarnings("rawtypes")
public void accumulateAll(Map arg0) {
     for (Iterator it = arg0.entrySet().iterator(); it.hasNext(); ){
         Map.Entry entry =(Map.Entry) it.next();
         
         try {
            getJSONObject().accumulate((String)entry.getKey(), entry.getValue());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }
 }
 public void clear() {
     object = new JSONObject();
 }
 public boolean containsKey(Object arg0) {
     return getJSONObject().has((String) arg0);
 }
 public boolean containsValue(Object arg0) {
     for (Iterator<String> it = getJSONObject().keys(); it.hasNext();){
         String key = it.next();
         try {
            if (getJSONObject().get(key) == arg0){
                 return true;
             }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }
     
     return false;
 }
 
 /*
  * These function will return the JSON object on fail.
  * Maybe it would be beter to return null so we will have an indication that somthing failed
  */
 public JSONObject element(String arg0, boolean arg1) {
     try {
        return getJSONObject().put(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return getJSONObject();
 }
 public JSONObject element(String arg0, Collection arg1) {
     try {
        return getJSONObject().put(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return getJSONObject();
 }
 public JSONObject element(String arg0, double arg1) {
     try {
        return getJSONObject().put(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return getJSONObject();
 }
 public JSONObject element(String arg0, int arg1) {
     try {
        return getJSONObject().put(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return getJSONObject();
 }
 public JSONObject element(String arg0, long arg1) {
     try {
        return getJSONObject().put(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return getJSONObject();
 }
 public JSONObject element(String arg0, Map arg1) {
     try {
        return getJSONObject().put(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return getJSONObject();
 }
 public JSONObject element(String arg0, Object arg1) {
     try {
        return getJSONObject().put(arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return getJSONObject();
 }
 public JSONObject elementOpt(String arg0, Object arg1) {
     if ( arg0 != null && arg1 != null ){
         try {
            return getJSONObject().put(arg0, arg1);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }
     
     return null;
 }
 @TargetApi(Build.VERSION_CODES.GINGERBREAD)
public Set entrySet() {
     Set<SimpleEntry<String, Object>> return_set = new TreeSet<AbstractMap.SimpleEntry<String, Object>>();
     
     
     for (Iterator<String> it = getJSONObject().keys(); it.hasNext();){
         String key = it.next();
         
         try {
            return_set.add(new AbstractMap.SimpleEntry<String, Object>(key, getJSONObject().get(key)));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }
     
     
     return return_set;
 }
 public Object get(Object arg0) {
     try {
        return getJSONObject().get((String)arg0);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return null;
 }
 public Object get(String arg0) {
     try {
        return getJSONObject().get(arg0);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return null;
 }
 public boolean getBoolean(String arg0) throws JSONException {
     return getJSONObject().getBoolean(arg0);
 }
 public double getDouble(String arg0) throws JSONException {
     return getJSONObject().getDouble(arg0);
 }
 public int getInt(String arg0) throws JSONException {
     return getJSONObject().getInt(arg0);
 }
 public JSONArray getJSONArray(String arg0) throws JSONException {
     return getJSONObject().getJSONArray(arg0);
 }
 public JSONObject getJSONObject(String arg0) throws JSONException {
     return getJSONObject().getJSONObject(arg0);
 }
 public long getLong(String arg0) throws JSONException {
     return getJSONObject().getLong(arg0);
 }
 public String getString(String arg0) throws JSONException {
     return getJSONObject().getString(arg0);
 }
 public boolean has(String arg0) {
     return getJSONObject().has(arg0);
 }
 public Iterator keys() {
     return getJSONObject().keys();
 }
 public Set keySet() {
     Set<String> set = new TreeSet<String>();
     
     for (Iterator<String> it = getJSONObject().keys(); it.hasNext();){
         set.add(it.next());
     }
         
     return set;
 }
 public JSONArray names() {
     return getJSONObject().names();
 }
 public Object opt(String arg0) {
     return getJSONObject().opt(arg0);
 }
 public boolean optBoolean(String arg0, boolean arg1) {
     return getJSONObject().optBoolean(arg0, arg1);
 }
 public boolean optBoolean(String arg0) {
     return getJSONObject().optBoolean(arg0);
 }
 public double optDouble(String arg0, double arg1) {
     return getJSONObject().optDouble(arg0, arg1);
 }
 public double optDouble(String arg0) {
     return getJSONObject().optDouble(arg0);
 }
 public int optInt(String arg0, int arg1) {
     return getJSONObject().optInt(arg0, arg1);
 }
 public int optInt(String arg0) {
     return getJSONObject().optInt(arg0);
 }
 public JSONArray optJSONArray(String arg0) {
     return getJSONObject().optJSONArray(arg0);
 }
 public JSONObject optJSONObject(String arg0) {
     return getJSONObject().optJSONObject(arg0);
 }
 public long optLong(String arg0, long arg1) {
     return getJSONObject().optLong(arg0, arg1);
 }
 public long optLong(String arg0) {
     return getJSONObject().optLong(arg0);
 }
 public String optString(String arg0, String arg1) {
     return getJSONObject().optString(arg0, arg1);
 }
 public String optString(String arg0) {
     return getJSONObject().optString(arg0);
 }
 public Object put(Object arg0, Object arg1) {
     try {
        return getJSONObject().accumulate((String) arg0, arg1);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     
     return null;
 }
 public void putAll(Map arg0) {
     for (Iterator<String> it = arg0.keySet().iterator(); it.hasNext();){
         String key = it.next();
         try {
            getJSONObject().put(key, arg0.get(key));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }
 }
 public Object remove(Object arg0) {
     return getJSONObject().remove((String)arg0);
 }
 public Object remove(String arg0) {
     return getJSONObject().remove(arg0);
 }
 public int size() {
     return getJSONObject().length();
 }
 
 public Collection values() {
     Collection c = new ArrayList();
     for (Iterator<String> it = getJSONObject().keys(); it.hasNext();){
         String key = it.next();
         try {
            c.add(getJSONObject().get(key));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }
     return c;
 }
 public boolean isEmpty() {
     return getJSONObject().length() == 0;
 }
}
