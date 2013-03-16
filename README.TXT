CouchDroid
---------


Introduction
------------
After looking into CouchDB, I attempted to use the existing couchdb4j project from
Marcus R. Breese (mbreese@gmail.com) on GitHub.  However, the original couchdb4j isn't 
fit to work as an android libray

So, since that wasn't going to work, I set about to modify couchdb4j so it will run on android.

Usage
-----
The API is prity much couchdb4j APO.  There are 5 main objects:
CouchSession, CouchDatabase, CouchDocument, CouchView, and CouchViewResult.  The session is the main connection 
to the CouchDB server.  You retrieve database instances from the CouchSession.  You can
get/create/update CoucDocuments to a CouchDatabase.  You can execute a CouchView on a CouchDatabase
to get your results in the form of a CouchViewResult (which is actually a type of CouchDocument).

CouchSession
-------
Using a CouchSession, you can list, get, create, and delete databases.

CouchDatabase
--------
Using a Database, you can create, get, delete, and update CouchDocuments.  You can also
use them to perform CouchView operations that return CouchViewResults.

CouchDocuments
---------
CouchDocuments are JSONObject backed.  They contain a JSONObject, and that is how all of their
properties are stored.

CouchView
----
A view is a javascript function that is executed to filter your documents on the server.  Since
they don't have to return full documents, the CouchView returns a CouchViewResult.  From the CouchViewResult, you can
retrieve a List<CouchDocument> of what the view returns.  From these CouchDocuments, you can retrieve the full 
CouchDocument from your database, if you wish.

To explain it in SQL terms, you can retrieve as much or as little of a row as you want in each query.
A Document is like the complete row of a table, and the View is like a SQL SELECT. (This is a gross
over simplification, but you get the idea.



Example
-------

String output = new String("");
CouchSession main_session = new CouchSession("192.168.1.100", 5984);
CouchDatabase census_db = main_session.getDatabase("mydatabase");
CouchViewResults q_results = census_db.view("myapp/myview");

List<CouchDocument> doc_list = q_results.getResults();
		
for (CouchDocument d: doc_list){
    output += d.toString();
}

Requirements
------------
All libraries which are required are part of the Android run time inviroment
License
-------
CouchDroid is licensed under the terms of the Apache 2.0 license as listed in the 
LICENSE.TXT file.


