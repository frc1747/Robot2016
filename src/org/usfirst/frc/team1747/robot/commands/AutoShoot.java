package org.usfirst.frc.team1747.robot.commands;

import org.usfirst.frc.team1747.robot.Robot;
import org.usfirst.frc.team1747.robot.SDController;
import org.usfirst.frc.team1747.robot.subsystems.DriveTrain;
import org.usfirst.frc.team1747.robot.subsystems.DriveTrainPID;
import org.usfirst.frc.team1747.robot.subsystems.Intake;
import org.usfirst.frc.team1747.robot.subsystems.Scooper;
import org.usfirst.frc.team1747.robot.subsystems.Shooter;
import org.usfirst.frc.team1747.robot.subsystems.Flashlight;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class AutoShoot extends Command {

	private DriveTrain driveTrain;
	private Shooter shooter;
	private Flashlight flashlight;
	private Intake intake;
	private Scooper scooper;
	private NetworkTable networkTable;
	private double startTime;
	private SDController.Positions position;
	private double turnValue;
	private DriverStation driverStation;
	private DriveTrainPID drivePID;

	int count = 0;
	int countTarget = 16;

	public AutoShoot() {
		driveTrain = Robot.getDriveTrain();
		shooter = Robot.getShooter();
		intake = Robot.getIntake();
		scooper = Robot.getScooper();
		flashlight = Robot.getFlashlight();
		drivePID = Robot.getDriveTrainPID();
		networkTable = NetworkTable.getTable("imageProcessing");
		driverStation = DriverStation.getInstance();
		requires(driveTrain);
		requires(shooter);
		requires(intake);
		requires(scooper);
		requires(flashlight);
		requires(drivePID);
	}

	protected void initialize() {
		position = Robot.getSd().getAutonPosition();
		startTime = -1;
		flashlight.turnOffFlashlight();
		driveTrain.resetGyro();
		turnValue = driveTrain.getAutonTurn();
		drivePID.pidDisable();
	}

	protected void execute() {
		//Ensure scooper is at lower limit
		if (!scooper.isAtLowerLimit()) {
			scooper.moveScooperDown();
		} else {
			scooper.scooperStop();
		}
		
		//Ensure intake is lifted up
		/*if (!intake.isAtTop()) {
			intake.moveLiftUp();
		} else {
			intake.liftStop();
		}*/

		//For auton, make sure that the position is valid
		if (position != SDController.Positions.NOTHING) {
			double turnAngle = SmartDashboard.getNumber("OffCenterDegreesX");
			double realDr = SmartDashboard.getNumber("RealDr");
			double realXr = SmartDashboard.getNumber("RealXr");
			
			double xrTol = 2.625;
			double drTol = 5;
			double drTarget = 130;
			double xrTarget = -2.1;
			double angleOffset = Math.atan2(xrTarget, drTarget);
			double realTurnAngle = turnAngle + angleOffset;
			//String direction = networkTable.getString("ShootDirection", "robotUnknown");
			String direction = "unknown";
			if(SmartDashboard.getBoolean("targetFound")) {
				direction = "shoot";
				//Currently prioritizing left/right over forward/backward
				if(realDr > drTarget + drTol) direction = "forward";
				if(realDr < drTarget - drTol) direction = "backward";
				if(realXr > xrTarget + xrTol) direction = "left";
				if(realXr < xrTarget - xrTol) direction = "right";
			}
			
			if (SmartDashboard.getBoolean("LastSecondShot", false) && driverStation.isAutonomous()
					&& !direction.equals("robotUnknown") && driverStation.getMatchTime() < 3) {
				direction = "shoot";
			}
			SmartDashboard.putString("direction", direction);
			if(direction.equals("shoot") && (!drivePID.isPidEnabled() || drivePID.isAtTarget() || (drivePID.getPidOutput() < .08 && drivePID.getPidOutput() > -0.08))) {
				drivePID.pidDisable();
				count = 0;
				driveTrain.arcadeDrive(0, 0);
				if(!shooter.isPidEnabled()) {
					shooter.setSetpoint(shooter.getTargetShooterSpeed());
					shooter.pidEnable();
				}
				if(shooter.isAtTarget()) {
					intake.intakeBall();
					if(startTime == -1) startTime = System.currentTimeMillis();
				}
			}
			else {
				startTime = -1;
				shooter.pidDisable();
				shooter.setSpeed(0.0);
				if(direction.equals("left") || direction.equals("right")) {
					if (!drivePID.isPidEnabled()) {
						//driveTrain.arcadeDrive(0, 0);
						count++;
						System.out.println("Incrementing counter");
						if(count > countTarget) {
							driveTrain.resetGyro();
							//double cameraAngle = networkTable.getNumber("GyroAngle", 0.0) * 1.9;
							drivePID.setSetpoint(realTurnAngle);
							drivePID.pidEnable();
							count = 0;
							System.out.println("count = 0; location 1");
						}
					}
					if (drivePID.isAtTarget()) {
						drivePID.pidDisable();
						driveTrain.resetGyro();
						count = 0;
						System.out.println("count = 0; location 2");
					}
				} else {
					//if (drivePID.isPidEnabled() && drivePID.isAtTarget() || (drivePID.getPidOutput() < .10 && drivePID.getPidOutput() > -0.10)) {
					drivePID.pidDisable();
					count = 0;
					System.out.println("count = 0; location 3");
					//}
					//if (direction.equals("forward") && !drivePID.isPidEnabled()) {
					if (direction.equals("forward")) {
						driveTrain.tankDrive(0.25, 0.27);
					}
					//} else if (direction.equals("backward") && !drivePID.isPidEnabled()) {
					else if (direction.equals("backward")) {
						driveTrain.tankDrive(-0.25, -0.27);
					}
					//} else if (direction.equals("unknown") && !drivePID.isPidEnabled()) {
					else if (direction.equals("unknown")) {
						//drivePID.pidDisable();
						if (position == SDController.Positions.ONE || position == SDController.Positions.TWO
								|| position == SDController.Positions.THREE) {
							driveTrain.arcadeDrive(0, turnValue * 0.85);
						} else {
							driveTrain.arcadeDrive(0, -turnValue * 0.85);
						}
					}
				}
			}
			SmartDashboard.putNumber("drive pid count", count);
			
			//////////// old stuff
			/*
			String direction = networkTable.getString("ShootDirection", "robotUnknown");

			if (SmartDashboard.getBoolean("LastSecondShot", false) && driverStation.isAutonomous()
					&& !direction.equals("robotUnknown") && driverStation.getMatchTime() < 3) {
				direction = "shoot";
			}

			if(direction.equals("shoot") && (!drivePID.isPidEnabled() || drivePID.isAtTarget() || (drivePID.getPidOutput() < .08 && drivePID.getPidOutput() > -0.08))) {
				drivePID.pidDisable();
				driveTrain.arcadeDrive(0, 0);
				if(!shooter.isPidEnabled()) {
					shooter.setSetpoint(shooter.getTargetShooterSpeed());
					shooter.pidEnable();
				}
				if(shooter.isAtTarget()) {
					intake.intakeBall();
					if(startTime == -1) startTime = System.currentTimeMillis();
				}
			}
			else {
				startTime = -1;
				shooter.pidDisable();
				shooter.setSpeed(0.0);
				if(direction.equals("left") || direction.equals("right")) {
					if (!drivePID.isPidEnabled()) {
						//driveTrain.arcadeDrive(0, 0);
						driveTrain.resetGyro();
						double cameraAngle = networkTable.getNumber("GyroAngle", 0.0) * 1.9;
						drivePID.setSetpoint(cameraAngle);
						drivePID.pidEnable();
					}
					if (drivePID.isAtTarget()) {
						drivePID.pidDisable();
						driveTrain.resetGyro();
					}
				} else {
					//if (drivePID.isPidEnabled() && drivePID.isAtTarget() || (drivePID.getPidOutput() < .10 && drivePID.getPidOutput() > -0.10)) {
					drivePID.pidDisable();
					//}
					//if (direction.equals("forward") && !drivePID.isPidEnabled()) {
					if (direction.equals("forward")) {
						driveTrain.tankDrive(0.3, 0.32);
					}
					//} else if (direction.equals("backward") && !drivePID.isPidEnabled()) {
					else if (direction.equals("backward")) {
						driveTrain.tankDrive(-0.3, -0.32);
					}
					//} else if (direction.equals("unknown") && !drivePID.isPidEnabled()) {
					else if (direction.equals("unknown")) {
						//drivePID.pidDisable();
						if (position == SDController.Positions.ONE || position == SDController.Positions.TWO
								|| position == SDController.Positions.THREE) {
							driveTrain.arcadeDrive(0, 1.15 * turnValue);
						} else {
							driveTrain.arcadeDrive(0, 1.15 * -turnValue);
						}
					}
				}
			}
			*/
			///////////////////////////
		}
	}

	protected boolean isFinished() {
		return (startTime != -1 && System.currentTimeMillis() - startTime > 600) || position == SDController.Positions.NOTHING;
	}

	protected void end() {
		drivePID.pidDisable();
		count = 0;
		driveTrain.arcadeDrive(0, 0);
		shooter.setSpeed(0.0);
		shooter.pidDisable();
		intake.rollerStop();
	}

	protected void interrupted() {
		end();
	}
}

//// Below is the Master AutoShoot code
/*
package org.usfirst.frc.team1747.robot.commands;

import org.usfirst.frc.team1747.robot.Robot;
import org.usfirst.frc.team1747.robot.SDController;
import org.usfirst.frc.team1747.robot.subsystems.DriveTrain;
import org.usfirst.frc.team1747.robot.subsystems.Flashlight;
import org.usfirst.frc.team1747.robot.subsystems.Intake;
import org.usfirst.frc.team1747.robot.subsystems.Scooper;
import org.usfirst.frc.team1747.robot.subsystems.Shooter;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class AutoShoot extends Command {

	private static final double stallTime = 800, radsThreshold = .95;
	private DriveTrain drive;
	private Shooter shooter;
	private Flashlight flashlight;
	private Intake intake;
	private Scooper scooper;
	private NetworkTable networkTable;
	private double startTime;
	private SDController.Positions position;
	private double turnValue;
	private double turnTime;
	private DriverStation driverStation;
	private boolean reset = false;

	// sets up AutoShoot
	public AutoShoot() {
		drive = Robot.getDriveTrain();
		shooter = Robot.getShooter();
		flashlight = Robot.getFlashlight();
		intake = Robot.getIntake();
		scooper = Robot.getScooper();
		networkTable = NetworkTable.getTable("imageProcessing");
		SmartDashboard.putNumber("StallTime", stallTime);
		SmartDashboard.putNumber("RadsThreshold", radsThreshold);
		SmartDashboard.putNumber("ShooterBaseline", 41.3);
		SmartDashboard.putBoolean("LastSecondShot", false);
		driverStation = DriverStation.getInstance();
		requires(shooter);
		requires(drive);
		requires(intake);
		requires(scooper);
	}

	// initializes AutoShoot then prints out that it is running
	protected void initialize() {
		position = Robot.getSd().getAutonPosition();
		startTime = -1;
		turnTime = -1;
		turnValue = drive.getAutonTurn();
		drive.resetGyro();
		flashlight.turnOffFlashlight();
	}

	protected void execute() {
		// Lowers scooper if not at lower limit
		if (!scooper.isAtLowerLimit()) {
			scooper.moveScooperDown();
		} else {
			scooper.scooperStop();
		}
		if (!intake.isAtTop()) {
			intake.moveLiftUp();
		} else {
			intake.liftStop();
		}
		if (position != SDController.Positions.NOTHING) {
			String direction = networkTable.getString("ShootDirection", "robotUnknown");
			if (turnTime - System.currentTimeMillis() <= -100) {
				double shooterRads = networkTable.getNumber("ShootRads", 0.0);
				turnTime = System.currentTimeMillis() + angleToTime(shooterRads);
				SmartDashboard.putNumber("angleToTime", angleToTime(shooterRads));
				reset = false;
			}
			if (SmartDashboard.getBoolean("LastSecondShot", false) && driverStation.isAutonomous()
					&& !direction.equals("robotUnknown") && driverStation.getMatchTime() < 3) {
				direction = "shoot";
			}
			// double boxDistance = networkTable.getNumber("ShootDistance", 0);
			SmartDashboard.putNumber("TimeLeftofTurn", turnTime - System.currentTimeMillis());
			if (direction.equals("left")) {
				//if (leftShooter.isPidEnabled() || rightShooter.isPidEnabled()) {
				shooter.pidDisable();
				//}
				shooter.setSpeed(0);
				if (turnTime - System.currentTimeMillis() >= 0) {
					drive.arcadeDrive(0.0, (-turnValue) * (driverStation.isAutonomous() ? 1 : 1.1));
				} else {
					drive.arcadeDrive(0, 0);
					if (!reset) {
						networkTable.putNumber("ShootRads", 0.0);
						reset = true;
					}
					// turnTime = -1;
				}
				startTime = -1;
			} else if (direction.equals("right")) {
				//if (leftShooter.isPidEnabled() || rightShooter.isPidEnabled()) {
				shooter.pidDisable();
				//}
				shooter.setSpeed(0.0);
				if (turnTime - System.currentTimeMillis() >= 0) {
					drive.arcadeDrive(0.0, (turnValue) * (driverStation.isAutonomous() ? 1 : 1.1));
				} else {
					drive.arcadeDrive(0, 0);
					if (!reset) {
						networkTable.putNumber("ShootRads", 0.0);
						reset = true;
					}
					// turnTime = -1;
				}
				startTime = -1;
			} else if (direction.equals("forward")) {
				//if (leftShooter.isPidEnabled() || rightShooter.isPidEnabled()) {
				shooter.pidDisable();
				//}
				shooter.setSpeed(0);
				drive.arcadeDrive(0.25, 0.0);
				startTime = -1;
			} else if (direction.equals("backward")) {
				//if (leftShooter.isPidEnabled() || rightShooter.isPidEnabled()) {
				shooter.pidDisable();
				//}
				shooter.setSpeed(0);
				drive.arcadeDrive(-0.25, 0.0);
				startTime = -1;
			} else if (direction.equals("shoot")) {
				drive.arcadeDrive(0, 0);

				if (startTime == -1) {
					startTime = System.currentTimeMillis();
				}
				if (startTime != -1 && System.currentTimeMillis() - startTime > 500 && (!shooter.isPidEnabled())) {
					shooter.setSetpoint(shooter.getTargetShooterSpeed());
					shooter.pidEnable();
				}
				//if (startTime != -1 && System.currentTimeMillis() - startTime > 750 && leftShooter.isAtTarget() && rightShooter.isAtTarget()) {
				if(shooter.isAtTarget()) {
					intake.intakeBall();
				}
			} else if (direction.equals("unknown")) {
				// Add Lift If 1
				if (position == SDController.Positions.ONE || position == SDController.Positions.TWO
						|| position == SDController.Positions.THREE) {
					drive.arcadeDrive(0, 1.3 * turnValue);
				} else {
					drive.arcadeDrive(0, 1.3 * -turnValue);
				}
			}
		}
	}

	// returns true if auto mode is done, if not it returns false; uses
	// startTime, position, and the current system time
	protected boolean isFinished() {
		return (startTime != -1 && System.currentTimeMillis() - startTime > 2250)
				|| position == SDController.Positions.NOTHING;
	}

	// ends shoot and arcadeDrive
	protected void end() {
		drive.arcadeDrive(0, 0);
		intake.rollerStop();
		shooter.setSpeed(0);
		shooter.pidDisable();
	}

	protected void interrupted() {
		end();
	}

	private double angleToTime(double shooterRads) {
		if (shooterRads == 0.0) {
			return -100;
		}
		return Math.max(
				(SmartDashboard.getNumber("RadsThreshold", radsThreshold) - shooterRads)
						* SmartDashboard.getNumber("StallTime", stallTime),
				SmartDashboard.getNumber("ShooterBaseline", 41.3));
	}

}
*/