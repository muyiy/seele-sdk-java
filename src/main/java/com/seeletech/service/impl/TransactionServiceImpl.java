package com.seeletech.service.impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.seeletech.model.RawTx;
import com.seeletech.model.SeeleSignature;
import com.seeletech.model.Transaction;
import com.seeletech.model.dto.AddTransactionDTO;
import com.seeletech.model.dto.SignTransactionDTO;
import com.seeletech.util.HttpResult;
import com.seeletech.util.bean.BeanUtil;
import com.seeletech.util.constant.HttpClientConstant;
import com.seeletech.util.exception.BaseException;
import com.seeletech.util.hash.HashUtil;
import com.seeletech.util.http.HttpClientUitl;
import com.seeletech.util.request.RequestUtil;
import org.apache.commons.lang3.StringUtils;
import org.ethereum.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


public class TransactionServiceImpl{

    public String sign(SignTransactionDTO transactionDTO) throws BaseException {
        HttpResult httpResult = new HttpResult();
        String msg = checkBlank(transactionDTO);
        if(!StringUtils.isEmpty(msg)){
            httpResult.setErrMsg(msg);
            return JSON.toJSONString(httpResult);
        }
        Transaction tx = null;
        try {
            tx = generateTransaction(transactionDTO);
            httpResult.setResult(BeanUtil.objectToMap(tx));
        } catch (Exception e) {
            httpResult.setErrMsg("objectToMap failed:"+e.getMessage());
            return JSON.toJSONString(httpResult);
        }
        return JSON.toJSONString(httpResult);
    }


    public Transaction generateTransaction(SignTransactionDTO transactionDTO) throws BaseException {
        String privatekey = transactionDTO.getPrivateKey();
        if (privatekey.startsWith("0x")) {
            privatekey = privatekey.substring(2);
        }
        RawTx rawTx = transactionDTO.getRawTx();
        Transaction tx = new Transaction(rawTx);
        String sig = "";
        try {
            byte[] hashBuffer = HashUtil.BeanTohash(tx);
            tx.setHash("0x" + Hex.toHexString(hashBuffer));
            ECKey key = ECKey.fromPrivate(Hex.decode(privatekey));
            ECKey.ECDSASignature signature = key.sign(hashBuffer);
            sig = Base64.getEncoder().encodeToString(signature.toByteArray());
        }catch(Exception e){
            throw new BaseException("generateTx failed:"+e.getMessage());
        }
        SeeleSignature seeleSignature = new SeeleSignature();
        seeleSignature.setSig(sig);
        tx.setSignature(seeleSignature);
        return tx;
    }

    public HttpResult addTx(String methodName, AddTransactionDTO transaction, HttpResult httpResult,String uri){
        String msg = checkBlank(transaction);
        if(!StringUtils.isEmpty(msg)){
            httpResult.setErrMsg(msg);
            return httpResult;
        }
        String requestJson = null;
        try {
            requestJson = RequestUtil.getRequestJson(methodName, transaction);
        } catch (JsonProcessingException e) {
            httpResult.setErrMsg("requestJson is valid:"+e.getMessage());
            return httpResult;
        }
        return HttpClientUitl.httpPostWithJson(requestJson, uri, HttpClientConstant.TIMEOUT,null,null);
    }

    private String checkBlank(Object obj) {
        String msg = "";
        if(obj instanceof  AddTransactionDTO){
            AddTransactionDTO transaction = (AddTransactionDTO)obj;
            if(StringUtils.isEmpty(transaction.getHash())){
                msg = "transaction hash is empty";
            }else if(StringUtils.isEmpty(transaction.getSignature().getSig())){
                msg = "transaction signature is empty";
            }
        }
        if(obj instanceof  SignTransactionDTO){
            SignTransactionDTO transactionDTO = (SignTransactionDTO) obj;
            if(StringUtils.isEmpty(transactionDTO.getRawTx().getFrom())){
                msg = "transaction from address is empty";
            }else if(StringUtils.isEmpty(transactionDTO.getRawTx().getTo())){
                msg = "transaction to address is empty";
            }
        }
        return msg;
    }


    public String sendTx(SignTransactionDTO signTransactionDTO,String uri) {
        HttpResult httpResult = new HttpResult();
        String msg = checkBlank(signTransactionDTO);
        if(!StringUtils.isEmpty(msg)){
            httpResult.setErrMsg(msg);
            return JSON.toJSONString(httpResult);
        }
        AddTransactionDTO addTransactionDTO = getAddTransactionDTO(signTransactionDTO);
        if(addTransactionDTO != null ){
            httpResult =  this.addTx("addTx",addTransactionDTO,httpResult,uri);
        }else{
            httpResult.setErrMsg("error:addTransactionDTO is null");
        }
        return JSON.toJSONString(httpResult);
    }

    public String gettxbyhash(String hash,String uri) {
        String requestJson = null;
        HttpResult httpResult = new HttpResult();
        try {
            requestJson = RequestUtil.getRequestJson("getTransactionByHash", hash);
        } catch (JsonProcessingException e) {
            httpResult.setErrMsg("requestJson is valid:"+e.getMessage());
            return JSON.toJSONString(httpResult);
        }
         httpResult =  HttpClientUitl.httpPostWithJson(requestJson, uri, HttpClientConstant.TIMEOUT,null,null);
         return JSON.toJSONString(httpResult);
    }


    public String key() {
        HttpResult httpResult = new HttpResult();
        ECKey key = new ECKey();
        Map map = new HashMap();
        map.put("public key","0x"+Hex.toHexString(key.getAddress()));
        map.put("private key","0x"+Hex.toHexString(key.getPrivKeyBytes()));
        httpResult.setResult(map);
        return JSON.toJSONString(httpResult);
    }
    private AddTransactionDTO getAddTransactionDTO(SignTransactionDTO signTransactionDTO){
        Transaction transaction = null;
        try{
            transaction = this.generateTransaction(signTransactionDTO);
            AddTransactionDTO addTransactionDTO = new AddTransactionDTO();
            addTransactionDTO.setData(transaction.getData());
            addTransactionDTO.setHash(transaction.getHash());
            addTransactionDTO.setSignature(transaction.getSignature());
            return addTransactionDTO;
        }catch(Exception e){
            return null;
        }
    }

//    public static void main(String[] args){
//        long begin = System.currentTimeMillis();
//        TransactionServiceImpl transactionServiceImpl = new TransactionServiceImpl();
//        SignTransactionDTO signTransactionDTO = new SignTransactionDTO();
//        signTransactionDTO.setPrivateKey("0xd738b0c1198e55050f754bdf0f824ee4febd962a6b751faab86c081ad5033b0d");
//        RawTx rawTx = new RawTx();
//        rawTx.setTo("");//0x0000000000000000000000000000000000000000
//        rawTx.setFrom("0xb265a2e04087a9a83492ffe191316f46b4730751");
//        rawTx.setAmount(0);
//        rawTx.setAccountNonce(0);
//        rawTx.setTimestamp(0);
//        rawTx.setPayload("0x60806040527fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff60015534801561003457600080fd5b5033600360006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550610f0b806100856000396000f300608060405260043610610083576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680632d142a99146100885780632de2c4da146100bc578063390e61541461015457806348d952e0146101b25780634eef2f46146101dd578063ab91695e1461021b578063f25b3f9914610246575b600080fd5b6100a660048036038101908080359060200190929190505050610295565b6040518082815260200191505060405180910390f35b3480156100c857600080fd5b5061013e6004803603810190808035906020019092919080359060200190929190803590602001909291908035906020019082018035906020019080806020026020016040519081016040528093929190818152602001838360200280828437820191505050505050919291929050505061042c565b6040518082815260200191505060405180910390f35b61019c6004803603810190808035906020019092919080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610968565b6040518082815260200191505060405180910390f35b3480156101be57600080fd5b506101c7610b40565b6040518082815260200191505060405180910390f35b6102056004803603810190808035906020019092919080359060200190929190505050610b46565b6040518082815260200191505060405180910390f35b34801561022757600080fd5b50610230610e3d565b6040518082815260200191505060405180910390f35b34801561025257600080fd5b5061027160048036038101908080359060200190929190505050610e43565b60405180848152602001838152602001828152602001935050505060405180910390f35b600080600090506001341015610356577f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd816040518080602001838152602001806020018381038352600e8152602001807f676574426c6f636b486561646572000000000000000000000000000000000000815250602001838103825260168152602001807f4552525f4d4f4e45595f49534e4f545f454e4f55474800000000000000000000815250602001935050505060405180910390a1809150610426565b600360009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166108fc349081150290604051600060405180830381858888f193505050501580156103be573d6000803e3d6000fd5b506000600260008581526020019081526020016000206001015413156103e357600190505b7f9f4ffac40aa756625bacf23a6a8fd81d386dfb2878ef3b572d73d00f454f003e8382604051808381526020018281526020019250505060405180910390a18091505b50919050565b60008060009050600360009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561053b577f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd81604051808060200183815260200180602001838103835260108152602001807f73746f7265426c6f636b48656164657200000000000000000000000000000000815250602001838103825260138152602001807f4552525f53544f52455f585f52454c4159455200000000000000000000000000815250602001935050505060405180910390a180915061095f565b600054841415156105f7577f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd81604051808060200183815260200180602001838103835260108152602001807f73746f7265426c6f636b48656164657200000000000000000000000000000000815250602001838103825260118152602001807f4552525f4e4f5f505245565f424c4f434b000000000000000000000000000000815250602001935050505060405180910390a180915061095f565b600154851315156106b3577f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd81604051808060200183815260200180602001838103835260108152602001807f73746f7265426c6f636b48656164657200000000000000000000000000000000815250602001838103825260188152602001807f4552525f424c4f434b5f414c52454144595f4558495354530000000000000000815250602001935050505060405180910390a180915061095f565b600060026000888152602001908152602001600020600101541315610783577f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd81604051808060200183815260200180602001838103835260108152602001807f73746f7265426c6f636b486561646572000000000000000000000000000000008152506020018381038252601d8152602001807f4552525f424c4f434b5f484153485f414c52454144595f455849535453000000815250602001935050505060405180910390a180915061095f565b60008351141561083e577f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd81604051808060200183815260200180602001838103835260108152602001807f73746f7265426c6f636b48656164657200000000000000000000000000000000815250602001838103825260198152602001807f4552525f5452414e53414354494f4e535f49535f454d50545900000000000000815250602001935050505060405180910390a180915061095f565b608060405190810160405280878152602001868152602001858152602001848152506002600088815260200190815260200160002060008201518160000155602082015181600101556040820151816002015560608201518160030190805190602001906108ad929190610e6d565b509050508560008190555084600181905550600190507f3d553c6a50657421fdcfde6c3ab85068b6c074a10dfc0073f149d98980d0395786868686856040518086815260200185815260200184815260200180602001838152602001828103825284818151815260200191508051906020019060200280838360005b83811015610944578082015181840152602081019050610929565b50505050905001965050505050505060405180910390a18091505b50949350505050565b6000806000806000925061097c8787610b46565b9150600082141515610a89578490508073ffffffffffffffffffffffffffffffffffffffff1663e6cb35af88886040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083815260200182815260200192505050602060405180830381600087803b158015610a0257600080fd5b505af1158015610a16573d6000803e3d6000fd5b505050506040513d6020811015610a2c57600080fd5b810190808051906020019092919050505050600192507f84ed50b3e2027837051c6eaa0464f66293959fe6892b7a62576d1ebcd53931e18684604051808381526020018281526020019250505060405180910390a1829350610b36565b7f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd83604051808060200183815260200180602001838103835260108152602001807f52656c61795472616e73616374696f6e00000000000000000000000000000000815250602001838103825260108152602001807f4552525f52454c41595f56455249465900000000000000000000000000000000815250602001935050505060405180910390a18293505b5050509392505050565b60015481565b600080606060008092506001341015610c0a577f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd83604051808060200183815260200180602001838103835260088152602001807f7665726966795478000000000000000000000000000000000000000000000000815250602001838103825260168152602001807f4552525f4d4f4e45595f49534e4f545f454e4f55474800000000000000000000815250602001935050505060405180910390a1829350610e34565b60066001540360026000888152602001908152602001600020600101541215610cde577f2889a9dad0676c5d3968f68aee3cf08ec189617986e3bc741824fabbae1dfddd83604051808060200183815260200180602001838103835260088152602001807f76657269667954780000000000000000000000000000000000000000000000008152506020018381038252601d8152602001807f4552525f434f4e4649524d4154494f4e535f4c4553535f5448414e5f36000000815250602001935050505060405180910390a1829350610e34565b600360009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166108fc349081150290604051600060405180830381858888f19350505050158015610d46573d6000803e3d6000fd5b5060026000878152602001908152602001600020600301805480602002602001604051908101604052809291908181526020018280548015610da757602002820191906000526020600020905b815481526020019060010190808311610d93575b50505050509150600090505b8151811015610df157848282815181101515610dcb57fe5b906020019060200201511415610de45760019250610df1565b8080600101915050610db3565b7f7a2933ac2a256db068a8aec8c8977f9866040648de7e973edfb5387e64a3d66a8584604051808381526020018281526020019250505060405180910390a18293505b50505092915050565b60005481565b60026020528060005260406000206000915090508060000154908060010154908060020154905083565b828054828255906000526020600020908101928215610ea9579160200282015b82811115610ea8578251825591602001919060010190610e8d565b5b509050610eb69190610eba565b5090565b610edc91905b80821115610ed8576000816000905550600101610ec0565b5090565b905600a165627a7a72305820fe072090d013b0fe073954d6379f9f21b04b42bce53ca7a5ab469c3b0631bbf10029");
//        rawTx.setGasPrice(1);
//        rawTx.setGasLimit(3000000);
//        signTransactionDTO.setRawTx(rawTx);
//        transactionServiceImpl.sign(signTransactionDTO);
////        String b= transactionServiceImpl.key();
////        System.out.println(b);
////        System.out.println(System.currentTimeMillis()-begin);
//    }
}
