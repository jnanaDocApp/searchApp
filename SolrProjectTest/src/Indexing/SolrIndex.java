package Indexing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class SolrIndex {

	private static HttpSolrServer solr;
	static UpdateRequest req = new UpdateRequest(); 
	static List<String> myFilesToIndex = new ArrayList<String>();

	public static void main(String[] args) throws IOException, SAXException, TikaException, SolrServerException {
		long startTime = System.nanoTime();
		try {
			 solr = new HttpSolrServer("http://localhost:8983/solr/star");
			 
			solr.deleteByQuery( "*:*" );			
			String[] folderPaths={"C:\\newFiles\\"};
			
			for(int j=0;j<folderPaths.length;j++){				
			File folder = new File(folderPaths[j]);
			//System.out.println(folderPaths[j]);			
			File[] listOfFiles = folder.listFiles();			
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					String myFilePath = listOfFiles[i].getAbsolutePath();
					String myFileName = listOfFiles[i].getName();
					processDocument(myFilePath, myFileName);  
				} else if (listOfFiles[i].isDirectory()) {		    	  
					myFilesToIndex.add(listOfFiles[i].getAbsolutePath());
				}
			}		
			

			while(!myFilesToIndex.isEmpty()){
				folder = new File(myFilesToIndex.get(0));
				getFileNames(folder.listFiles());
				myFilesToIndex.remove(0);
			}
			}
			
			@SuppressWarnings("unused")
			UpdateResponse rsp = req.process( solr );
			
		}
		catch  (Exception ex) {
			//System.out.println(ex.getMessage());
	
		}	
		
		long endTime = System.nanoTime();
		System.out.println("Took :"+(endTime - startTime) + " ns"); 
		System.out.println("Took "+TimeUnit.MINUTES.convert((endTime - startTime), TimeUnit.NANOSECONDS)+" minutes to index");
		solr.commit();
	}


	private static void getFileNames(File[] listFiles) {
		for (int i = 0; i < listFiles.length; i++) {
			if (listFiles[i].isFile()) {
				String myFilePath = listFiles[i].getAbsolutePath();
				String myFileName = listFiles[i].getName();
				if(myFileName.contains(myFileName))
				{
				}
				processDocument(myFilePath,myFileName);  
			} else if (listFiles[i].isDirectory()) {		    	  
				myFilesToIndex.add(listFiles[i].getAbsolutePath());
			}
		}

	}



	private static void processDocument(String pathfilename, String myFileName)  {

		try {
			 //parse method parameters
			FileInputStream input = new FileInputStream(new File(pathfilename));			
			ContentHandler textHandler = new BodyContentHandler(10*1024*1024);
			Metadata meta = new Metadata();
			Parser parser = new AutoDetectParser(); 
			ParseContext context = new ParseContext();	 
			parser.parse(input, textHandler, meta, context);
			
			
			UUID guid = java.util.UUID.randomUUID();
			String docid = guid.toString();
			//System.out.println(docid);
			String doctitle = myFileName;
			//System.out.println(doctitle);
			String fileName = FilenameUtils.removeExtension(doctitle);
			//System.out.println(fileName);
			String docurl = pathfilename;
			//System.out.println(docurl);
			String doccontent = textHandler.toString();
			String textOrg=doccontent.toLowerCase().replace("\n", " ");
			String trimContent=textOrg.replaceAll("\\.+",".").replaceAll("\\-+", "-");
			
			if(trimContent==null||trimContent.isEmpty()){	
				trimContent=doctitle;
				//System.out.println("---------------content was empty----------");
			}
			String author=meta.get(TikaCoreProperties.CREATOR);
			if(author==null||author.isEmpty()){
				author="No Author";
			}
			String dateFormat="";
			Date date=meta.getDate(TikaCoreProperties.MODIFIED);
			if(date==null){
				dateFormat="No Date available";
			}
			else{
			 dateFormat=new SimpleDateFormat("dd-MMM-yyyy").format(date);
			}
			//System.out.println("date :  "+date);
			//System.out.println(author);
			
			String size = "size";
			//call to index
			indexDocument(docid, doctitle, author, docurl, trimContent, size,dateFormat,fileName);
		}
		catch  (Exception ex) {	
			System.out.println("error tika: "+ex.getMessage());
		}
	}

	private static void indexDocument(String docid, String doctitle, String doccreator, String docurl, String textOrg, String size,String dateFormat,String fileName) 
	{

		try {
			SolrInputDocument doc = new SolrInputDocument();
			doc.addField("id", docid);
			doc.addField("title", doctitle);
			doc.addField("author", doccreator);		
			doc.addField("url", docurl);		
			doc.addField("content", textOrg);
			doc.addField("dateCal", dateFormat);
			doc.addField("fileName", fileName);
			req.setAction( UpdateRequest.ACTION.COMMIT, false, false );
			req.add( doc ); 
			 UpdateResponse resp  = solr.add(doc);	
			 //solr.commit();
			 System.out.println("STATUS  :  "+resp.getStatus());
		} 
		catch (Exception ex) {
			//System.out.println("error :"+ex.getMessage());
		}
	}	
}
