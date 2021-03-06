package aysntask;

import java.util.ArrayList;
import java.util.List;

import net.tsz.afinal.FinalDb;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.activity.HomeActivity;
import com.activity.LoginAct;

import util.FileOperator;
import util.SpUtil;
import util.Util;
import vo.Content;
import vo.LoginSucsess;
import vo.Myself;
import config.Const;

public class LoginTask extends BaseTask<Myself, Void, LoginSucsess> {

    private LoginAct act;

    public static String SEND_NAME;

    public static String currentName = "";

    public LoginTask(LoginAct context) {
        super(context);
        act = context;
    }

    @Override
    public LoginSucsess doExecute(Myself info) throws Exception {
        final String url = Const.BASE_URL + "login?n=" + info.getChannelId() + "&" + "p="
                + info.getPass();
        HttpHeaders reqtHeaders = new HttpHeaders();
        List<MediaType> acceptMediaTypes = new ArrayList<MediaType>();
        acceptMediaTypes.add(MediaType.APPLICATION_JSON);
        reqtHeaders.setAccept(acceptMediaTypes);

        HttpEntity<?> requestEntity = new HttpEntity<Object>(reqtHeaders);

        RestTemplate rest = new RestTemplate();
        rest.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        ResponseEntity<LoginSucsess> resp = rest.exchange(url, HttpMethod.GET, requestEntity,
                LoginSucsess.class);
        if (resp.getStatusCode() == HttpStatus.OK) {
        	SpUtil sp=new SpUtil(act);
        	sp.setValue(Const.USER_NAME, info.getName());
            return resp.getBody();
        }
        return null;
    }

    @Override
    public void doResult(LoginSucsess result) throws Exception {
        if (result != null) {
            currentName = result.getMyself().getName();
            FinalDb db = FinalDb.create(act, FileOperator.getDbPath(act), true);
            db.save(result.getMyself());
            if (!Util.isEmpty(result.getOfflineMsgs())) {
                for (Content offlineMsg : result.getOfflineMsgs()) {
                    // 说明是群组发来的离线消息
                    if (offlineMsg.getGrouppTag() != 0) {
                        List<Content> msgs = HomeActivity.groupMsgs.get(offlineMsg.getGrouppTag());
                        if (Util.isEmpty(msgs)) {
                            msgs = new ArrayList<Content>();
                        }
                        msgs.add(offlineMsg);
                        HomeActivity.groupMsgs.put(offlineMsg.getGrouppTag(), msgs);
                    } else {
                        // 私聊的离线消息
                        List<Content> msgs = HomeActivity.singleMsgs.get(offlineMsg.getSendId());
                        if (Util.isEmpty(msgs)) {
                            msgs = new ArrayList<Content>();
                        }
                        msgs.add(offlineMsg);
                        HomeActivity.singleMsgs.put(offlineMsg.getSendId(), msgs);
                    }
                }
                
            }
            //私聊本地未读消息
            List<Content> unReadLoaclSingleMsgs =db.findAllByWhere(Content.class, "isRead = 'false' and grouppTag = 0 ");
            if(!Util.isEmpty(unReadLoaclSingleMsgs)){
                for(Content content:unReadLoaclSingleMsgs){
                    List<Content> msgs = HomeActivity.singleMsgs.get(content.getSendId());
                    if (Util.isEmpty(msgs)) {
                        msgs = new ArrayList<Content>();
                    }
                    content.setIsLocalMsg("true");
                    msgs.add(content);
                    HomeActivity.singleMsgs.put(content.getSendId(), msgs);
                }
            }
          //群聊本地未读消息
            List<Content> unReadLoaclGroupMsgs =db.findAllByWhere(Content.class, "isRead = 'false' and grouppTag != 0 ");
            if(!Util.isEmpty(unReadLoaclGroupMsgs)){
                for(Content content:unReadLoaclGroupMsgs){
                    List<Content> msgs = HomeActivity.groupMsgs.get(content.getGrouppTag());
                    if (Util.isEmpty(msgs)) {
                        msgs = new ArrayList<Content>();
                    }
                    content.setIsLocalMsg("true");
                    msgs.add(content);
                    HomeActivity.groupMsgs.put(content.getGrouppTag(), msgs);
                }
            }
            act.skip(HomeActivity.class, result);
        } else {
            toast(act, "登陆失败,请检查输入信息");
        }
    }
}
