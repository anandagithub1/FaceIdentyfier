
package fidelity;

import org.jdesktop.application.Action;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.List;
import Luxand.*;
import Luxand.FSDK.*;
import Luxand.FSDKCam.*;
import java.io.File;
import java.util.Arrays;

/**
 * @author A546456-Ananda Kumar Tokappa
 */

public class FaceView extends FrameView {

    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel mainPanel;


    public final Timer drawingTimer;
    private HCamera cameraHandle;

    final int programStateRemember = 1;
    final int programStateRecognize = 2;
    private int programState = programStateRecognize;

    private String TrackerMemoryFile = "tracker.dat";
    private int mouseX ;
    private int mouseY ;

    HTracker tracker ;

    private JFrame frame;
    private FSDK.FSDK_FaceTemplate.ByReference faceTemplateF;
    private FSDK.FSDK_FaceTemplate.ByReference faceTemplateT;
    private float Similarity[];
    static int indentLevel =-1;

    String dirPath="E:/Nexus5_Pics_Vids/pics";

    @Override
    public JFrame getFrame(){

        if(frame == null){
            String title="                    FIDELTY  FACE  IDENTYFIER";
             frame = new JFrame(title);
             frame.setName("Fidelity Face Identyfier");
             frame.setSize(400,400);
             frame.setResizable(false);
             frame.setLocation(400, 200);
        }

        return frame;
    }


    public FaceView(SingleFrameApplication app) {
        super(app);

        tracker = new HTracker();

        initComponents();

        final JPanel mainFrame = this.mainPanel;

        try {
            int r = FSDK.ActivateLibrary("lpPla47swXkC7yMEKT9qf1DD7juxcXyVvzqW1ZCZQR7RRvz802RLcPwANc6ItswpiwXHuUQ3obGSaE/dp+6v82Srh1wqR4koVfcxjUtc+8We5XZTBX6EZR4L8uhPe2bmS9KoL4VkHlbCjMWeZOPU9V936FA3hKmwu0F7Fvaqiec=");
            if (r != FSDK.FSDKE_OK){
                JOptionPane.showMessageDialog(mainPanel, "Please run the License Key Wizard (Start - Luxand - FaceSDK - License Key Wizard)", "Error activating FaceSDK", JOptionPane.ERROR_MESSAGE);
                System.exit(r);
            }
        }
        catch(java.lang.UnsatisfiedLinkError e) {
            JOptionPane.showMessageDialog(mainPanel, e.toString(), "Link Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        FSDK.Initialize();

        
        if (FSDK.FSDKE_OK != FSDK.LoadTrackerMemoryFromFile(tracker, TrackerMemoryFile)) 
            FSDK.CreateTracker(tracker);

       
        int err[] = new int[1];
        err[0] = 0;
        FSDK.SetTrackerMultipleParameters(tracker, "HandleArbitraryRotations=false; DetermineFaceRotationAngle=false; InternalResizeWidth=100; FaceDetectionThreshold=5;", err);

        FSDKCam.InitializeCapturing();

        TCameras cameraList = new TCameras();
        int count[] = new int[1];
        FSDKCam.GetCameraList(cameraList, count);
        if (count[0] == 0){
            JOptionPane.showMessageDialog(mainFrame, "Please attach a camera");
            System.exit(1);
        }

        String cameraName = cameraList.cameras[0];

        FSDK_VideoFormats formatList = new FSDK_VideoFormats();
        FSDKCam.GetVideoFormatList(cameraName, formatList, count);
        FSDKCam.SetVideoFormat(cameraName, formatList.formats[0]);

        cameraHandle = new HCamera();
        int r = FSDKCam.OpenVideoCamera(cameraName, cameraHandle);
        if (r != FSDK.FSDKE_OK){
            JOptionPane.showMessageDialog(mainFrame, "Error opening camera");
            System.exit(r);
        }


       drawingTimer = new Timer(40, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HImage imageHandle = new HImage();
                if (FSDKCam.GrabFrame(cameraHandle, imageHandle) == FSDK.FSDKE_OK){
                    Image awtImage[] = new Image[1];
                    if (FSDK.SaveImageToAWTImage(imageHandle, awtImage, FSDK_IMAGEMODE.FSDK_IMAGE_COLOR_24BIT) == FSDK.FSDKE_OK){

                        BufferedImage bufImage = null;
                        TFacePosition.ByReference facePosition = new TFacePosition.ByReference();

                        long[] IDs = new long[256];
                        long[] faceCount = new long[1];

                        FSDK.FeedFrame(tracker, 0, imageHandle, faceCount, IDs);
                        for (int i=0; i<faceCount[0]; ++i) {
                            FSDK.GetTrackerFacePosition(tracker, 0, IDs[i], facePosition);

                            int left = facePosition.xc - (int)(facePosition.w * 0.6);
                            int top = facePosition.yc - (int)(facePosition.w * 0.5);
                            int w = (int)(facePosition.w * 1.2);

                            bufImage = new BufferedImage(awtImage[0].getWidth(null), awtImage[0].getHeight(null), BufferedImage.TYPE_INT_ARGB);
                            Graphics gr = bufImage.getGraphics();
                            gr.drawImage(awtImage[0], 0, 0, null);
                            gr.setColor(Color.green);

    			    String [] name = new String[1];
			    int res = FSDK.GetAllNames(tracker, IDs[i], name, 65536); 

			    if (FSDK.FSDKE_OK == res && name[0].length() > 0) { 
                                gr.setFont(new Font("Arial", Font.BOLD, 16));
                                FontMetrics fm = gr.getFontMetrics();
                                java.awt.geom.Rectangle2D textRect = fm.getStringBounds(name[0], gr);
                                gr.drawString(name[0], (int)(facePosition.xc - textRect.getWidth()/2), (int)(top + w + textRect.getHeight()));
                            }

                            if (mouseX >= left && mouseX <= left + w && mouseY >= top && mouseY <= top + w){
                                gr.setColor(Color.blue);

                                if (programStateRemember == programState) {
                                    if (FSDK.FSDKE_OK == FSDK.LockID(tracker, IDs[i]))
                                    {
                                        faceTemplateF = new FSDK.FSDK_FaceTemplate.ByReference();
                                        FSDK.GetFaceTemplate(imageHandle, faceTemplateF);
                                        templateMatcher(faceTemplateF);
                                    }
                                }
                            }
                            programState = programStateRecognize;

                            gr.drawRect(left, top, w, w); 
                        }

                       mainFrame.getRootPane().getGraphics().drawImage((bufImage != null) ? bufImage : awtImage[0], 0, 0, null);
                    }
                    FSDK.FreeImage(imageHandle); 
                }
            }
        });
    }

public void templateMatcher(FSDK.FSDK_FaceTemplate.ByReference faceTemplateF){

    File files[];
     File folder = new File(dirPath);
     files = folder.listFiles();
     HImage imageT = new HImage();
     faceTemplateT = new FSDK.FSDK_FaceTemplate.ByReference();
     Float simi=0.00f;
     String pic=null;
     Arrays.sort(files);
     Similarity=new float[1];

     for(int i=0,n=files.length;i<n;i++){
        for(int indent=0;indent<indentLevel;indent++){
        System.out.print("");
        }

        System.out.println(files[i].toString());
        FSDK.LoadImageFromFile(imageT, files[i].toString());
        FSDK.GetFaceTemplate(imageT, faceTemplateT);

        FSDK.MatchFaces(faceTemplateF, faceTemplateT, Similarity);

        System.out.println("Picture Similarity is : "+Similarity[0]*100+"%");
     
     if(simi<Similarity[0]){
     simi=Similarity[0];
     pic=files[i].toString();
     }
     }

     PicShower m=new PicShower(pic);
     JFrame f=new JFrame();

     f.add(m);
     f.setTitle("               IDENTYFIED PHOTO");
     f.setLocation(840, 200);
     f.setSize(410, 380);
     f.setResizable(false);
     f.setVisible(true);
     f.pack();

}
 
    @SuppressWarnings("unchecked")
   
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        mainPanel.setName("mainPanel"); 
        mainPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                mainPanelMouseEntered(evt);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                mainPanelMouseExited(evt);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                mainPanelMouseReleased(evt);
            }
        });
        mainPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                mainPanelMouseMoved(evt);
            }
        });

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(fidelity.FaceIdentyfier.class).getContext().getActionMap(FaceView.class, this);
        jButton1.setAction(actionMap.get("buttonStart")); 
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(fidelity.FaceIdentyfier.class).getContext().getResourceMap(FaceView.class);
        jButton1.setText("START"); 
        jButton1.setToolTipText("START");
        jButton1.setName("START"); 

        jLabel1.setText(resourceMap.getString("jLabel1.text")); 
        jLabel1.setName("jLabel1"); 

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap(309, Short.MAX_VALUE)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(101, 101, 101))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap(298, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel1.getAccessibleContext().setAccessibleName(resourceMap.getString("jLabel1.AccessibleContext.accessibleName")); // NOI18N

        setComponent(mainPanel);
    }

    private void mainPanelMouseReleased(java.awt.event.MouseEvent evt) {
        programState = programStateRemember;
    }

    private void mainPanelMouseEntered(java.awt.event.MouseEvent evt) {
    }

    private void mainPanelMouseExited(java.awt.event.MouseEvent evt) {
        mouseX = 0;
        mouseY = 0;
    }

    private void mainPanelMouseMoved(java.awt.event.MouseEvent evt) {
        mouseX = evt.getX();
        mouseY = evt.getY();
    }

    @Action
    public void buttonStart() {
        this.jButton1.setEnabled(false);
        drawingTimer.start();
    }

    public void saveTracker(){
        FSDK.SaveTrackerMemoryToFile(tracker, TrackerMemoryFile);
    }

    public void closeCamera(){
        FSDKCam.CloseVideoCamera(cameraHandle);
        FSDKCam.FinalizeCapturing();
        FSDK.Finalize();
    }


}