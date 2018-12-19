package org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn;

import android.content.Context;
import android.support.annotation.NonNull;

import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.admin.Daemon2AdminAA;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.app.DTNClient;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.app.DTNUI;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.cla.Daemon2CLA;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.fragmentmanager.Daemon2FragmentManager;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.manager.DTNManager;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.manager.Daemon2Managable;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.peerdiscoverer.Daemon2PeerDiscoverer;

public final class BWDTN {
    private static RadAppAA radAppAA = null;
    private static RadAdminAA radAdminAA = null;
    private static RadFragMgr radFragMgr = null;
    private static RadDiscoverer radDiscoverer = null;
    private static RadCLA radCLA = null;
    private static RadManager radManager = null;
    private static RadDaemon radDaemon = null;

    private BWDTN() {}
    
    public static DTNClient getDTNClient() {
        if (radDaemon == null) return null;
        else return radAppAA;
    }
    
    public static DTNManager getDTNManager() {
        if (radDaemon == null) return null;
        else return radManager;
    }
    
    public static void init(@NonNull Context context, @NonNull DTNUI ui) {
        if (radDaemon == null) {
            radDaemon = new RadDaemon();
            
            radAppAA = new RadAppAA(ui, radDaemon);
            radManager = new RadManager(radDaemon);
    
            radDaemon.setCLA(getCLA(context));
            radDaemon.setDiscoverer(getPeerDiscoverer(context));
            radDaemon.setAppAA(radAppAA);
            radDaemon.setAdminAA(getAdminAA());
            radDaemon.setFragmentManager(getFragmentManager());
            radDaemon.setManagables(new Daemon2Managable[]{radDiscoverer, radCLA});
        }
    }
    
    private static Daemon2FragmentManager getFragmentManager() {
        if (radFragMgr == null) {
            radFragMgr = new RadFragMgr();
        }
        return radFragMgr;
    }
    
    private static Daemon2AdminAA getAdminAA() {
        if (radAppAA == null) radAdminAA = new RadAdminAA(radDaemon);
        return radAdminAA;
    }
    
    private static Daemon2PeerDiscoverer getPeerDiscoverer(@NonNull Context context) {
        if (radCLA == null) radCLA = new RadCLA(radDaemon, context);
        if (radDiscoverer == null) radDiscoverer = new RadDiscoverer(radDaemon, radCLA, context);
        return radDiscoverer;
    }
    
    private static Daemon2CLA getCLA(@NonNull Context context) {
        if (radCLA == null) radCLA = new RadCLA(radDaemon, context);
        return radCLA;
    }
}