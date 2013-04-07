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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
* The results of a view request is just a specialized Document object.
* You can use ViewResults to retrieve information about the results (such as the 
* number of rows returned).
* <p>
* The ViewResults document contains a JSONArray named "rows".  This JSON array contains
* further sub-Documents.  These documents include the _id and _rev of the matched Documents as
* well as any other fields that the View function returns -- it is not the full Document.
* <p>
* In order to retrieve the full document, you must call database.getDocument(id).
* 
* @author mbreese
*
*/
public class CouchViewResults extends CouchDocument {

    private CouchView calledView;

    /**
     * Builds the ViewResults object from the given JSON object. (called only from Database.view())
     * This shouldn't be called by user code. 
     * @param calledView
     * @param obj
     */
    CouchViewResults(CouchView calledView, JSONObject obj) {
        super(obj);
        this.calledView=calledView;
    }
     
    /**
     * Retrieves a list of documents that matched this View.
     * These documents only contain the data that the View has returned (not the full document).
     * <p>
     * You can load the remaining information from Document.reload();
     * 
     * @return
    */
    public List<CouchDocument> getResults() {
        JSONArray ar = null;
        try {
            android.util.Log.e("Census", getJSONObject().toString());
            ar = getJSONObject().getJSONArray("rows");
        } catch (JSONException e) {
            return null;
        }
        List<CouchDocument> docs = new ArrayList<CouchDocument>(ar.length());
        for (int i=0 ; i< ar.length(); i++) {
            try {
                if (ar.get(i)!=null && !ar.getString(i).equals("null")) {
                     CouchDocument d = new CouchDocument(ar.getJSONObject(i));
                     d.setDatabase(database);
                     docs.add(d);
                 }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return docs;
    
    }

    /**
     * The new that created this results list.
     * @return
     */
    public CouchView getView() {
        return calledView;
    }
}

