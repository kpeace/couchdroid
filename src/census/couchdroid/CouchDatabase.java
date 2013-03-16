

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

import org.json.*;

import java.io.IOException;
import java.net.URLEncoder;
//import com.fourspaces.couchdb.util.JSONUtils;
//import static com.fourspaces.couchdb.util.JSONUtils.urlEncodePath;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
* This represents a particular database on the CouchDB server
* <p/>
* Using this object, you can get/create/update/delete documents.
* You can also call views (named and adhoc) to query the underlying database.
*
* @author mbreese
*/
public class CouchDatabase {
    //Log log = LogFactory.getLog(Database.class);
    private final String name;
    private int documentCount;
    private int updateSeq;

    private CouchSession session;

    private static final String VIEW = "/_view/";
    private static final String DESIGN = "_design/";
    private static final String UPDATE = "/_update/";


/**
* C-tor only used by the Session object.  You'd never call this directly.
*
* @param json
* @param session
*/
    CouchDatabase(JSONObject json, CouchSession session) {
        String name_tmp = new String("");
        try {
            
            name_tmp = json.getString("db_name");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        name = name_tmp;
        
        try {
            documentCount = json.getInt("doc_count");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            updateSeq = json.getInt("update_seq");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        this.session = session;
    }

/**
* The name of the database
*
* @return
*/
public String getName() {
 return name;
}

/**
* The number of documents in the database <b>at the time that it was retrieved from the session</b>
* This number probably isn't accurate after the initial load... so if you want an accurate
* assessment, call Session.getDatabase() again to reload a new database object.
*
* @return
*/
public int getDocumentCount() {
 return documentCount;
}

/**
* The update seq from the initial database load.  The update sequence is the 'revision id' of an entire database. Useful for getting all documents in a database since a certain revision
*
* @return
* @see getAllDocuments()
*/
public int getUpdateSeq() {
 return updateSeq;
}

/**
* Runs the standard "_all_docs" view on this database
*
* @return ViewResults - the results of the view... this can be iterated over to get each document.
*/
public CouchViewResults getAllDocuments() {
 return view(new CouchView("_all_docs"), false);
}

/**
* Gets all design documents
*
* @return ViewResults - all design docs
*/     
public CouchViewResults getAllDesignDocuments() {
    CouchView v = new CouchView("_all_docs");
   v.startKey = "%22_design%2F%22";
   v.endKey = "%22_design0%22";
   v.includeDocs = Boolean.TRUE;
   return view(v, false);
}

/**
* Runs the standard "_all_docs" view on this database, with count
*
* @return ViewResults - the results of the view... this can be iterated over to get each document.
*/
public CouchViewResults getAllDocumentsWithCount(int count) {
    CouchView v = new CouchView("_all_docs");
    v.setCount(count);
    return view(v, false);
}

/**
* Runs "_all_docs_by_update_seq?startkey=revision" view on this database
*
* @return ViewResults - the results of the view... this can be iterated over to get each document.
*/
public CouchViewResults getAllDocuments(int revision) {
 return view(new CouchView("_all_docs_by_seq?startkey=" + revision), false);
}

/**
* Runs a named view on the database
* This will run a view and apply any filtering that is requested (reverse, startkey, etc).
*
* @param view
* @return
*/
public CouchViewResults view(CouchView view) {
 return view(view, true);
}

/**
* Runs a view, appending "_view" to the request if isPermanentView is true.
* *
*
* @param view
* @param isPermanentView
* @return
*/
private CouchViewResults view(final CouchView view, final boolean isPermanentView) {
 String url = null;
 if (isPermanentView) {
   String[] elements = view.getFullName().split("/");
   url = this.name + "/" + ((elements.length < 2) ? elements[0] : DESIGN + elements[0] + VIEW + elements[1]);
 }
 else {
   url = this.name + "/" + view.getFullName();
 }

 CouchResponse resp = session.get(url, view.getQueryString());
 if (resp.isOk()) {
     CouchViewResults results = new CouchViewResults(view, resp.getBodyAsJSONObject());
   results.setDatabase(this);
   return results;
 }
 return null;

}

/**
* Runs a named view <i>Not currently working in CouchDB code</i>
*
* @param fullname - the fullname (including the document name) ex: foodoc:viewname
* @return
*/

public CouchViewResults view(String fullname) {
 return view(new CouchView(fullname), true);
}

/**
* Runs an ad-hoc view from a string
*
* @param function - the Javascript function to use as the filter.
* @return results
*/
public CouchViewResults adhoc(String function) {
 return adhoc(new CouchAdHocView(function));
}

/**
* Runs an ad-hoc view from an AdHocView object.  You probably won't use this much, unless
* you want to add filtering to the view (reverse, startkey, etc...)
*
* @param view
* @return
*/
public CouchViewResults adhoc(final CouchAdHocView view) {
    String adHocBody = "";
    try {
        adHocBody = new JSONStringer()
             .object()
               .key("map").value("\"" + view.getFunction() + "\"")
             .endObject()
             .toString();
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
    // Bugfix - include query string for adhoc views to support
    // additional view options (setLimit, etc)
    CouchResponse resp = session.post(name + "/_temp_view", adHocBody, view.getQueryString());
    if (resp.isOk()) {
        CouchViewResults results = new CouchViewResults(view, resp.getBodyAsJSONObject());
        results.setDatabase(this);
        return results;
    }
    else {
       //log.warn("Error executing view - " + resp.getErrorId() + " " + resp.getErrorReason());
    }
    return null;
}

/**
* Save a document at the given _id
* <p/>
* if the docId is null or empty, then this performs a POST to the database and retrieves a new
* _id.
* <p/>
* Otherwise, a PUT is called.
* <p/>
* Either way, a new _id and _rev are retrieved and updated in the Document object
*
* @param doc
* @param docId
*/
public void saveDocument(CouchDocument doc, String docId) throws IOException {
 CouchResponse resp;
 if (docId == null || docId.equals("")) {
   resp = session.post(name, doc.getJSONObject().toString());
 }
 else {
   resp = session.put(name + "/" + URLEncoder.encode(docId, "utf-8").replaceAll("%2F", "/"), doc.getJSONObject().toString());
 }

 if (resp.isOk()) {
   try {
     if (doc.getId() == null || doc.getId().equals("")) {
       doc.setId(resp.getBodyAsJSONObject().getString("id"));
     }
     doc.setRev(resp.getBodyAsJSONObject().getString("rev"));
   }
   catch (JSONException e) {
     e.printStackTrace();
   }
   doc.setDatabase(this);
 }
 else {
   //log.warn("Error adding document - " + resp.getErrorId() + " " + resp.getErrorReason());
   System.err.println("RESP: " + resp);
 }
}

/**
* Save a document w/o specifying an id (can be null)
*
* @param doc
*/
public void saveDocument(CouchDocument doc) throws IOException {
 saveDocument(doc, doc.getId());
}

public void bulkSaveDocuments(CouchDocument[] documents) throws IOException {
 CouchResponse resp = null;

 try {
    resp = session.post(name + "/_bulk_docs", new JSONObject().accumulate("docs", documents).toString());
} catch (JSONException e1) {
    // TODO Auto-generated catch block
    e1.printStackTrace();
}

 if (resp.isOk()) {
   // TODO set Ids and revs and name (db)
   final JSONArray respJsonArray = resp.getBodyAsJSONArray();
   JSONObject respObj = null;
   String id = null;
   String rev = null;
   for (int i = 0; i < documents.length; i++) {
     try {
        respObj = respJsonArray.getJSONObject(i);
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     try {
        id = respObj.getString("id");
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     try {
        rev = respObj.getString("rev");
    } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
     if (documents[i].getId().trim().length() == 0) {
       documents[i].setId(id);
       documents[i].setRev(rev);
     }
     else if (documents[i].getId().trim().length() > 0 && documents[i].getId().equals(id)) {
       documents[i].setRev(rev);
     }
     else {
       //log.warn("returned bulk save array in incorrect order, saved documents do not have updated rev or ids");
     }
     documents[i].setDatabase(this);
   }
 }
 else {
   //log.warn("Error bulk saving documents - " + resp.getErrorId() + " " + resp.getErrorReason());
 }
}

/**
* Retrieves a document from the CouchDB database
*
* @param id
* @return
*/
public CouchDocument getDocument(String id) throws IOException {
 return getDocument(id, null, false);
}

/**
* Retrieves a document from the database and asks for a list of it's revisions.
* The list of revision keys can be retrieved from Document.getRevisions();
*
* @param id
* @return
*/
public CouchDocument getDocumentWithRevisions(String id) throws IOException {
 return getDocument(id, null, true);
}

/**
* Retrieves a specific document revision
*
* @param id
* @param revision
* @return
*/
public CouchDocument getDocument(String id, String revision) throws IOException {
 return getDocument(id, revision, false);
}

/**
* Retrieves a specific document revision and (optionally) asks for a list of all revisions
*
* @param id
* @param revision
* @param showRevisions
* @return the document
*/
public CouchDocument getDocument(String id, String revision, boolean showRevisions) throws IOException {
 CouchResponse resp;
 CouchDocument doc = null;
 if (revision != null && showRevisions) {
   resp = session.get(name + "/" + URLEncoder.encode(id, "utf-8").replaceAll("%2F", "/"), "rev=" + revision + "&full=true");
 }
 else if (revision != null && !showRevisions) {
   resp = session.get(name + "/" + URLEncoder.encode(id, "utf-8").replaceAll("%2F", "/"), "rev=" + revision);
 }
 else if (revision == null && showRevisions) {
   resp = session.get(name + "/" + URLEncoder.encode(id, "utf-8").replaceAll("%2F", "/"), "revs=true");
 }
 else {
   resp = session.get(name + "/" + URLEncoder.encode(id, "utf-8").replaceAll("%2F", "/"));
 }
 if (resp.isOk()) {
   doc = new CouchDocument(resp.getBodyAsJSONObject());
   doc.setDatabase(this);
 }
 else {
   //log.warn("Error getting document - " + resp.getErrorId() + " " + resp.getErrorReason());
 }
 return doc;
}

/**
* Deletes a document
*
* @param d
* @return was the delete successful?
* @throws IllegalArgumentException for blank document id
*/
public boolean deleteDocument(CouchDocument d) throws IOException {

 if (d.getId().trim().length() == 0) {
   throw new IllegalArgumentException("cannot delete document, doc id is empty");
 }

 CouchResponse resp = session.delete(name + "/" +  URLEncoder.encode(d.getId(), "utf-8").replaceAll("%2F", "/") + "?rev=" + d.getRev());

 if (resp.isOk()) {
   return true;
 }
 else {
   //log.warn("Error deleting document - "+resp.getErrorId()+" "+resp.getErrorReason());
         return false;
     }
     
 }
 
/**
* Gets attachment
*
* @param id
* @param attachment attachment body
* @return attachment body
*/
 public String getAttachment(String id, String attachment) throws IOException {
     CouchResponse resp = session.get(name + "/" + URLEncoder.encode(id, "utf-8").replaceAll("%2F", "/") + "/" + attachment);
     return resp.getBody();
 }

/**
* Puts attachment to the doc
*
* @param id
* @param fname attachment name
* @param ctype content type
* @param attachment attachment body
* @return was the PUT successful?
*/
 public String putAttachment(String id, String fname, String ctype, String attachment) throws IOException {
     CouchResponse resp = session.put(name + "/" + URLEncoder.encode(id, "utf-8").replaceAll("%2F", "/") + "/" + fname, ctype, attachment);
     return resp.getBody();
 }
 
/**
* Update an existing document using a document update handler. Returns false if there is a failure
* making the PUT/POST or there is a problem with the CouchResponse.
* @author rwilson
* @param update
* @return
*/
 public boolean updateDocument(CouchUpdate update) {
   if ((update == null) || (update.getDocId() == null) || (update.getDocId().equals(""))) {
     return false;
   }
   
   String url = null;
   
   String[] elements = update.getName().split("/");
   url = this.name + "/" + ((elements.length < 2) ? elements[0] : DESIGN + elements[0] + UPDATE + elements[1]) + "/" + update.getDocId();
   
   if (update.getMethodPOST()) {
     try { 
       // Invoke the POST method passing the parameters in the body
       CouchResponse resp = session.post(url, "application/x-www-form-urlencoded", update.getURLFormEncodedString(), null);
       return resp.isOk();
     } catch (Exception e) {
       return false;
     }        
   } else {
     try {
       // Invoke the PUT method passing the parameters as a query string
       CouchResponse resp = session.put(url, null, null, update.getQueryString());
       return resp.isOk();
     } catch (Exception e) {
       return false;
     }        
   }
 }
 
 /**
  * Update an existing document using a document update handler and return the message body.
  * Returns null if the is problem with the PUT/POST or CouchResponse.
  * @author rwilson
  * @param update
  * @return
  */
   public String updateDocumentWithResponse(CouchUpdate update) {
     if ((update == null) || (update.getDocId() == null) || (update.getDocId().equals(""))) {
       return "";
     }
     
     String url = null;
     
     String[] elements = update.getName().split("/");
     url = this.name + "/" + ((elements.length < 2) ? elements[0] : DESIGN + elements[0] + UPDATE + elements[1]) + "/" + update.getDocId();
     
     if (update.getMethodPOST()) {
       try {
         // Invoke the POST method passing the parameters in the body
         CouchResponse resp = session.post(url, "application/x-www-form-urlencoded", update.getURLFormEncodedString(), null);
         return resp.getBody();
       } catch (Exception e) {
         return null;
       }
     } else {
       try {
         // Invoke the PUT method passing the parameters as a query string
         CouchResponse resp = session.put(url, null, null, update.getQueryString());
         return resp.getBody();
       } catch (Exception e) {
         return null;
       }
     }
   }
}
