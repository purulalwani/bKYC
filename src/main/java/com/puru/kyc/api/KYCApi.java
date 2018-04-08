package com.puru.kyc.api;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;

import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;

import com.puru.kyc.contract.KYCContract;
import com.puru.kyc.state.KYCState;
import com.puru.kyc.flow.KYCFlow;
import com.puru.kyc.model.KYC;
import net.corda.core.transactions.SignedTransaction;

// This API is accessible from /api/kyc. All paths specified below are relative to it.
@Path("kyc")
public class KYCApi {
    private final CordaRPCOps services;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary", "Network Map Service");

    public KYCApi(CordaRPCOps services) {
        this.services = services;
        this.myLegalName = services.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /*
     * Returns the name of the node providing this end-point.
     * GET Request::
     * http://localhost:10007/api/kyc/me
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() { return ImmutableMap.of("me", myLegalName); }

    /**
     * Returns all parties registered with the [NetworkMapService]. The names can be used to look up identities by
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = services.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }
    
    /*
     * Returns all kycs
     * GET Request::
     * http://localhost:10007/api/kyc/get-kycs
     */
    @GET
    @Path("get-kycs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<KYCState>> getKYCs() {
        return services.vaultQuery(KYCState.class).getStates();
    }

    /*
     * Search matching kycs based on user id
     * GET Request::
     * http://localhost:10007/api/kyc/<user_id>/get-kycs-by-userid
     */
    @GET
    @Path("{userId}/get-kycs-by-userid")
    @Produces(MediaType.APPLICATION_JSON)
    public List<KYC> getKYCsByUserId(@PathParam("userId") String userId) {
    	
    	List<KYC> returnRecords = new ArrayList<KYC>();
    	
    	List<StateAndRef<KYCState>> allRecords = services.vaultQuery(KYCState.class).getStates();
    	
    	for(int i=0; i<allRecords.size();i++){
    		
    		StateAndRef<KYCState> singleRecord = (StateAndRef<KYCState>) allRecords.get(i);
    		
    		KYCState state = (KYCState) singleRecord.getState().getData();
    		
    		if(state.getKYC().getUserId().equalsIgnoreCase(userId)){
    			returnRecords.add(state.getKYC());
    		}
    	}
    	// return only one record based on kycDate which is created last
    	KYC lastKYC = Collections.max(returnRecords, Comparator.comparing(KYC::getKycDate));
    	
    	returnRecords.clear();
    	returnRecords.add(lastKYC);
    	
        return returnRecords;
    }
    
    /*
     * Single party
     * http://localhost:10005/api/kyc/<HDFC>/create-kyc
     * PUT Request::
       {
    		"kycId": 111, "userId": "biksen", "userName": "Jiya Sen", "kycDate": "2017-02-09", "kycValidDate": "2019-09-15", "docId": "A001"
	   }
    */
   @PUT
   @Path("{party1}/create-kyc")
   public Response createKYC(KYC kyc, @PathParam("party1") String partyName1) throws InterruptedException, ExecutionException {
       final Set<Party> parties = services.partiesFromName(partyName1, true);
       


       if (parties.isEmpty()) {
           return Response.status(Response.Status.BAD_REQUEST).build();
       }

       final Party otherParty = parties.iterator().next();

       System.out.println("Party1............"+otherParty);
       
       System.out.println("Request received............"+kyc);

       final KYCState state = new KYCState(
               kyc,
               services.nodeInfo().getLegalIdentities().get(0),
               otherParty);

      try{
       // Initiate flow here. The line below blocks and waits for the flow to return.
       final SignedTransaction signedTx = services
               .startFlowDynamic(KYCFlow.Initiator.class, state, otherParty)
               .getReturnValue()
               .get();

       final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
       return Response.status(CREATED).entity(msg).build();

   } catch (Throwable ex) {
        final String msg = ex.getMessage();
        ex.printStackTrace();
        return Response.status(BAD_REQUEST).entity(msg).build();
    }
}

   
   @PUT
   @Path("{otherParty}/create-kyc-with-attachment")
   public Response createKYCWithAttachment(KYC kyc, @PathParam("otherParty") String otherPartyName) throws InterruptedException, ExecutionException {
	   
	   /** Get vault update status - tracker */
//	   Pair<List<StateAndRef<ContractState>>, rx.Observable<Update>> tr =services.qu;
//	   rx.Observable<Update> status1 = tr.component2();
//	   status1.subscribe(s -> System.out.println(s));
	   /*status1.just("Hello Jiya, world!")
	   .map(s -> s + " -Dan")
	   .map(s -> s.hashCode())
	   .map(i -> Integer.toString(i))
	   //.subscribe(testSubscriber);
	   .subscribe(s -> System.out.println(s)); // -*/
	   /** End tracker */

       final Set<Party> parties = services.partiesFromName(otherPartyName, true);



       if (parties.isEmpty()) {
           return Response.status(Response.Status.BAD_REQUEST).build();
       }

       final Party otherParty = parties.iterator().next();

       System.out.println("Party1............"+otherParty);

       System.out.println("Request received............"+kyc);

       final KYCState state = new KYCState(
               kyc,
               services.nodeInfo().getLegalIdentities().get(0),
               otherParty);
       
      /** Add attachment - Added attachment logic into KYCFlow.java */
       /** Read file directly from src/main/resources location */
       //InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("R-3083.zip");       
              
       String filePath = new File("").getAbsolutePath() + File.separator + kyc.getUserId() +"_kyc.zip";       
      
       System.out.println("Jiys's filePath......"+ filePath);
       InputStream in = null;
       try{
    	   byte[] bytes = "Hello, World!".getBytes("UTF-8");
    	   String encoded = Base64.getEncoder().encodeToString(bytes);
    	   byte[] decoded = Base64.getDecoder().decode(encoded);   	       	   
    	   FileOutputStream fop = new FileOutputStream(filePath);

    	   fop.write(decoded);
    	   fop.flush();
    	   fop.close();
    	  
           URL newFileURL = new File(filePath).toURI().toURL();
           //java.io.BufferedInputStream will be created by openStream()
           in = newFileURL.openStream();
           
       }catch(Exception e){
    	   e.printStackTrace();
       }
       
       System.out.println("File input stream created....."+in);    
       
       SecureHash attachmentHashValue =  services.uploadAttachment(in);       
       /** End attachment */

       // Initiate flow here. The line below blocks and waits for the flow to return.
       try{
           // Initiate flow here. The line below blocks and waits for the flow to return.
           final SignedTransaction signedTx = services
                   .startFlowDynamic(KYCFlow.Initiator.class, state, otherParty)
                   .getReturnValue()
                   .get();

           final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
           return Response.status(CREATED).entity(msg).build();

       } catch (Throwable ex) {
           final String msg = ex.getMessage();
           ex.printStackTrace();
           return Response.status(BAD_REQUEST).entity(msg).build();
       }
   }

   
}
