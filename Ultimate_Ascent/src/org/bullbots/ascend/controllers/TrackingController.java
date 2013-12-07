package org.bullbots.ascend.controllers;

import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.image.*;
import org.bullbots.ascend.hardware.Camera;

/**
 * @author Derik J
 */
public class TrackingController {
    
    class TrackingPidSource implements PIDSource {
        TrackingController controller;
        
        public TrackingPidSource(TrackingController controller) {
            this.controller = controller;
        }
        public double pidGet() {
            //System.out.println("TRACKING ERROR: " + controller.getTrackingError());
            return controller.getTrackingError();
        }
    }

    class DepthPidSource implements PIDSource{
        TrackingController controller;
        
        public DepthPidSource(TrackingController controller) {
            this.controller = controller;
        }
        public double pidGet() {
            return controller.getDepthError();
        }
    }
    
    Camera camera;
    ColorImage image;
    BinaryImage thresholdImage;
    BinaryImage convexImage;
    BinaryImage removeSmallObjectsImage;
    CriteriaCollection criteriaCollection = new CriteriaCollection();
    ParticleAnalysisReport[] particleReport;
    JoystickControl joystick;
    ParticleAnalysisReport highestRectangle;
    
    private boolean pressed = false;
    
    private final int RED_LOW = 0;
    private final int RED_HIGH = 255;
    private final int GREEN_LOW = 230;
    private final int GREEN_HIGH = 255;
    private final int BLUE_LOW = 0;
    private final int BLUE_HIGH = 255;
    private final double RECTANGULARITY = .85;
    
    private double trackingError = 0.0;
    private double depthError = 0.0;
    
    public TrackingController(JoystickControl joystick){
        try{
            this.joystick = joystick;
            camera = new Camera();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public double getDif(){
        return trackingError;
    }
    
    public void trackGoal(){
        try
        {
            image = camera.getCameraImage();
            
            if(image != null){
                thresholdImage = image.thresholdRGB(RED_LOW, RED_HIGH, GREEN_LOW, GREEN_HIGH, BLUE_LOW, BLUE_HIGH);
                convexImage = thresholdImage.convexHull(true);
                removeSmallObjectsImage = convexImage.removeSmallObjects(false, 4);
                removeSmallObjectsImage.particleFilter(criteriaCollection);
                particleReport = removeSmallObjectsImage.getOrderedParticleAnalysisReports();

                for(int i = 0; i  < particleReport.length; i++){
                    if(i == 0){
                        highestRectangle = particleReport[0];
                        for(int k = 0; k < particleReport.length; k++){
                            
                            if(particleReport[k].particleArea/(particleReport[k].boundingRectHeight*particleReport[k].boundingRectWidth) >= RECTANGULARITY){
                                highestRectangle = particleReport[k];
                                k = particleReport.length;
                                i = k + 1;
                            }
                        } 
                    }
                    else if(highestRectangle.center_mass_y < particleReport[i].center_mass_y && particleReport[i].particleArea/(particleReport[i].boundingRectHeight*particleReport[i].boundingRectWidth) >= RECTANGULARITY){
                        highestRectangle = particleReport[i];
                    }
                }

                if(highestRectangle != null){
                   trackingError = ((image.getWidth()/2) - highestRectangle.center_mass_x);  
                   calculateDepth();
                   //System.out.println(highestRectangle.center_mass_x);
                   //System.out.println("DIFF: " + trackingError);
                }

                if(joystick.getButton(1)){
                    if(!pressed){
                        removeSmallObjectsImage.write("/FilteredImage.png");
                        System.out.println("Writing image");
                    }
                    pressed = true;
                } 
                else{
                    pressed = false;
                }

                //System.out.println("Freeing images");
                
                image.free();
                thresholdImage.free();
                convexImage.free();
                removeSmallObjectsImage.free();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public void calculateDepth()
    {
        depthError = ((20 * 320) / (2 * highestRectangle.boundingRectHeight)) / Math.tan(24 * (Math.PI/180));
        System.out.println("Depth Error: " + depthError + " inches");
    }
    
    public double getTrackingError() {
        return trackingError;
    }
    
    public double getDepthError() {
        System.out.println("getting depth value: " + depthError);
        return depthError;
    }
    
    public TrackingPidSource getTrackingErrorSource(){
        return new TrackingPidSource(this);
    }
    
    public DepthPidSource getDepthErrorSource()
    {
        return new DepthPidSource(this);
    }
}