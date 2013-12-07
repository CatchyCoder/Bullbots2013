package org.bullbots.ascend.hardware;

/**
 *
 * @author Clay Kuznia
 */
public class Shooter
{
    private Cannon cannon = new Cannon();
    private Hopper hopper = new Hopper();
    
    public void shoot() {
	// If the first slot is loaded, fire
        if(hopper.slot1Full()) cannon.fire();
	else {
            cannon.setServoPosition(0);
	    // If there is a frisbee in slot 1, move it into slot 2
            if(hopper.slot2Full() && cannon.servosReady()) hopper.spinWheel();
            // If no frisbees are left, stop the wheels
	    else hopper.stopWheel();
        }
        
	// If all slots are full, stop the wheels
        if(!hopper.slot1Full() && !hopper.slot2Full() && cannon.servosReady()) cannon.stopWheels();
    }
    
    public Cannon getCannon() {
        return cannon;
    }
    
    public Hopper getHopper() {
        return hopper;
    }
}
