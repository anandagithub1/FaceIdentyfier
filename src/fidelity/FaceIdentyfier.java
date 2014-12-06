
package fidelity;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import java.awt.event.*;

/**
 * @author A546456-Ananda Kumar Tokappa
 */

public class FaceIdentyfier extends SingleFrameApplication {
    private FaceView faceView;

   @Override protected void startup() {
        faceView = new FaceView(this);
        show(faceView);
    }

  
    @Override protected void configureWindow(java.awt.Window root) {
        root.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                faceView.drawingTimer.stop();
                try{
                    Thread.sleep(40);
                }
                catch (java.lang.InterruptedException ex){
                    ex.printStackTrace();
                }
                faceView.saveTracker();
                faceView.closeCamera();
            }
        });
    }

   
    public static FaceIdentyfier getApplication() {
        return Application.getInstance(FaceIdentyfier.class);
    }

    
    public static void main(String[] args) {
        launch(FaceIdentyfier.class, args);
    }
}
