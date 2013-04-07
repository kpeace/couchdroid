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

import java.util.HashMap;
import java.util.Map;

/**
* The View is the mechanism for performing Querys on a CouchDB instance.
* The view can be named or ad-hoc (see AdHocView). (Currently [14 Sept 2007] named view aren't working in the 
* mainline CouchDB code... but this _should_ work.)
*<p>
* The View object exists mainly to apply filtering to the view.  Otherwise, views can be 
* called directly from the database object by using their names (or given an ad-hoc query).
* 
* @author mbreese
*
*/
public class CouchView {
    
    public class QueryArguments{
        public final static String KEY = "key";
        public final static String STARTKEY = "startkey";
        public final static String ENDKEY = "endkey";
        public final static String LIMIT = "limit";
        public final static String INCLUDEDOCS = "include_docs";
        
        private HashMap<String, String> query_arguments;
        
        QueryArguments(HashMap<String, String> new_query_arguments){
            // TODO add argument validation checking
            query_arguments = new HashMap<String, String>(new_query_arguments);
        }
        
        String getQueryAguments(){
            if (query_arguments.size() == 0){
                return null;
            }
            
            String query = new String();
            
            for (Map.Entry<String, String> entry : query_arguments.entrySet()){
                // non numeric values must be surrounded by " (quotes).
                String val = entry.getValue();
                try{
                    Integer.parseInt(val);
                }
                catch (NumberFormatException e){
                    if (val != null){
                        val = "\"" + val + "\"";
                    }
                }
                
                query += entry.getKey() + "=" + val + "&";
            }
            
            return query.substring(0, query.length() - 1);
        }
    }
    protected QueryArguments query_arguments = null;
 
    protected String name;
    protected CouchDocument document;
    protected String function;
 
    /**
     * Build a view given a document and a name
     * 
     * @param doc
     * @param name
     */
    public CouchView(CouchDocument doc, String name) {
        this.document=doc;
        this.name=name;
    }
 
 /**
  * Build a view given only a fullname ex: ("_add_docs", "_temp_view")
  * @param fullname
  */
 public CouchView(String fullname, HashMap<String, String> query_args) {
     this.name=fullname;
     this.document=null;
     
     setQueryArguments(query_args);
 }
 
 /**
  * Builds a new view for a document, a given name, and the function definition.
  * This <i>does not actually add it to the document</i>.  That is handled by
  * Document.addView()
  * <p>
  * This constructor should only be called by Document.addView();
  * 
  * @param doc
  * @param name
  * @param function
  */
 CouchView(CouchDocument doc, String name, String function) {
     this.name=name;
     this.document=doc;
     this.function=function;
 }
 
 /**
  * Accepts a Hash map of key = argument, value = argument_value
  * these will be appended to the view as "?argument1=argument_value1&argument2=argument_value2"
  */
 public void setQueryArguments(HashMap<String, String> new_query_arguments){
     query_arguments = new QueryArguments(new_query_arguments);
 }
 
 /**
  * Based upon settings, builds the queryString to add to the URL for this view.
  * 
  * @return
  */
 public String getQueryString() {
     return (query_arguments == null ? null : query_arguments.getQueryAguments());
 }
 
 
 /**
  * The name for this view (w/o doc id)
  * @return
  */
 public String getName() {
     return name;
 }
 /**
  * the full name for this view (w/ doc id, if avail)
  * in the form of : 
  * "docid:name"
  * or 
  * "name"
  * @return
  */
 public String getFullName() {
     return (document==null) ? name: document.getViewDocumentId()+"/"+name;
 }

 /**
  * The function definition for this view, if it is available.
  * @return
  */
 public String getFunction() {
     return function;
 }
 
}

