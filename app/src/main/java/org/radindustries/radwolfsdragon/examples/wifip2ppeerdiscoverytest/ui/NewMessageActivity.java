package org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.DConstants;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.R;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class NewMessageActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG
        = DConstants.MAIN_LOG_TAG + "_" + NewMessageActivity.class.getSimpleName();
    
    private String lifeTimeFromSettings;
    private String routingProtocolFromSettings;
    private String priorityClassFromSettings;
    
    private Messenger dtnServiceMessenger = null;
    private final ServiceConnection serviceConnection = new MKDTNServiceConnection();
    
    private class MKDTNServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            dtnServiceMessenger = new Messenger(service);
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            dtnServiceMessenger = null;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        initUI();
        bind();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        unbind();
    }
    
    private TextInputEditText toTIET;
    private TextInputEditText msgTIET;
    
    private void initUI() {
        toTIET = findViewById(R.id.to_eid_tiet);
        msgTIET = findViewById(R.id.msg_tiet);
        
        if (getIntent() != null) {
            Intent intent = getIntent();
            if (intent.getStringExtra(PeersActivity.TO_EID_TAG) != null) {
                toTIET.setText(intent.getStringExtra(PeersActivity.TO_EID_TAG));
            } else {
                toTIET.setText(R.string.dtn_256);
            }
        }
        
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View v) {
        if (TextUtils.isEmpty(toTIET.getText()) || TextUtils.isEmpty(msgTIET.getText())) {
            Toast.makeText(this, R.string.sending_err_msg, Toast.LENGTH_SHORT).show();
        }
        else {
            String recipient = Objects.requireNonNull(toTIET.getText()).toString();
            String message = Objects.requireNonNull(msgTIET.getText()).toString();
    
            sendDTNMessage(recipient, message);
    
            Toast.makeText(this, R.string.sending_msg_ok, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
    
    private void bind() {
        if (MKDTNService.isRunning(this)) {
            Intent intent = new Intent(this, MKDTNService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }
    
    private void unbind() {
        if (MKDTNService.isRunning(this)) {
            unbindService(serviceConnection);
        }
    }
    
    private void getDTNSettings() {
        if (MKDTNService.configFileDoesNotExist(this)) {
            MKDTNService.writeDefaultConfig(this);
        }
        MKDTNService.DTNConfig config = MKDTNService.getConfig(this);
        
        lifeTimeFromSettings = config.lifetime;
        routingProtocolFromSettings = config.routingProtocol;
        priorityClassFromSettings = config.priorityClass;
    }
    
    private void sendDTNMessage(String destination, String message) {
        getDTNSettings();
        
        Message msg = Message.obtain(null, MKDTNService.MSG_SEND);
        
        Bundle data = new Bundle();
        data.putString(MKDTNService.RECIPIENT_KEY, destination);
        data.putString(MKDTNService.TEXT_KEY, message);
        data.putString(MKDTNService.LIFETIME_KEY, lifeTimeFromSettings);
        data.putString(MKDTNService.PRIORITY_KEY, priorityClassFromSettings);
        data.putString(MKDTNService.PROTOCOL_KEY, routingProtocolFromSettings);
        
        msg.setData(data);
        
        sendMessageToDTNService(msg);
    }
    
    private void sendMessageToDTNService(Message msg) {
        if(dtnServiceMessenger != null) {
            try {
                dtnServiceMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Service down", e);
            }
        }
    }
}
