package org.usfirst.frc.team1747.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.PIDSourceType;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.usfirst.frc.team1747.robot.RobotMap;
import org.usfirst.frc.team1747.robot.SDLogger;
import org.usfirst.frc.team1747.robot.commands.TeleopDrive;

import java.util.LinkedList;

public class DriveTrain extends Subsystem implements SDLogger {
	static final double[] SIGMOIDSTRETCH = { 0.03, 0.06, 0.09, 0.1, 0.11, 0.12, 0.11, 0.1, 0.09, 0.06, 0.03 };
	DriveSide left, right;
	LinkedList<Double> straightTargetDeltas = new LinkedList<Double>();
	LinkedList<Double> rotationTargetDeltas = new LinkedList<Double>();
	double pStraightTarget = 0.0, pRotationTarget = 0.0, prevTargetStraight = 0.0, prevTargetRotation = 0.0;
	Solenoid glowLeft;
	Solenoid glowRight;

	// Sets up CANTalons for drive train
	public DriveTrain() {
		left = new DriveSide(RobotMap.LEFT_DRIVE_CIM_ONE, RobotMap.LEFT_DRIVE_CIM_TWO, RobotMap.LEFT_DRIVE_MINICIM,
				true);
		right = new DriveSide(RobotMap.RIGHT_DRIVE_CIM_ONE, RobotMap.RIGHT_DRIVE_CIM_TWO, RobotMap.RIGHT_DRIVE_MINICIM,
				false);
		glowLeft = new Solenoid(RobotMap.ROBOT_GLOW_LEFT);
		glowRight = new Solenoid(RobotMap.ROBOT_GLOW_RIGHT);
		turnOnGlow();
		// Left and right motors face each other, left is inverted
		for (int j = 0; j < SIGMOIDSTRETCH.length; j++) {
			straightTargetDeltas.add(0.0);
			rotationTargetDeltas.add(0.0);
		}
		SmartDashboard.putNumber("DriveTrain LP", .01);
		SmartDashboard.putNumber("DriveTrain LI", 0);
		SmartDashboard.putNumber("DriveTrain LD", 0);
		SmartDashboard.putNumber("DriveTrain RP", .01);
		SmartDashboard.putNumber("DriveTrain RI", 0);
		SmartDashboard.putNumber("DriveTrain RD", 0);
	}

	// Sets up the tank drive.
	public void tankDrive(double leftSpeed, double rightSpeed) {
		left.set(leftSpeed);
		right.set(rightSpeed);
	}

	public void arcadeDrive(double straight, double turn) {
		tankDrive(straight + turn, straight - turn);
	}

	// This is smooth drive. bush did 9II
	public void smoothDrive(double targetStraight, double targetRotation) {
		straightTargetDeltas.removeLast();
		rotationTargetDeltas.removeLast();
		straightTargetDeltas.addFirst(targetStraight - prevTargetStraight);
		rotationTargetDeltas.addFirst(targetRotation - prevTargetRotation);
		prevTargetStraight = targetStraight;
		prevTargetRotation = targetRotation;
		for (int i = 0; i < SIGMOIDSTRETCH.length; i++) {
			pStraightTarget += straightTargetDeltas.get(i) * SIGMOIDSTRETCH[i];
			pRotationTarget += rotationTargetDeltas.get(i) * SIGMOIDSTRETCH[i];
		}
		arcadeDrive(pStraightTarget, pRotationTarget);
	}

	@SuppressWarnings("Duplicates")
	public void plateauDrive(double straight, double turn) {
		if (Math.abs(straight) < .01) {
			straight = 0.0;
		} else if (Math.abs(straight) < 0.5) {
			straight = (Math.abs(straight) / straight) * 0.5 / (1 + Math.exp(-20.0 * (Math.abs(straight) - 0.2)));
		} else {
			straight = (Math.abs(straight) / straight)
					* (0.5 / (1 + Math.exp(-20.0 * (Math.abs(straight) - 0.8)) + 0.5));
		}
		if (Math.abs(turn) < .01) {
			turn = 0.0;
		} else if (Math.abs(turn) < 0.5) {
			turn = (Math.abs(turn) / turn) * 0.5 / (1 + Math.exp(-20.0 * (Math.abs(turn) - 0.2)));
		} else {
			turn = (Math.abs(turn) / turn) * (0.5 / (1 + Math.exp(-20.0 * (Math.abs(turn) - 0.8)) + 0.5));
		}
		SmartDashboard.putNumber("Straight", straight);
		SmartDashboard.putNumber("Turn", turn);
		tankDrive(straight + turn, straight - turn);
	}

	@Override
	public void initDefaultCommand() {
		setDefaultCommand(new TeleopDrive());
	}

	// This is a public void that logs smart dashboard.
	public void logToSmartDashboard() {
		SmartDashboard.putNumber("Left DriveTrain Speed", left.getSpeed());
		SmartDashboard.putNumber("Right DriveTrain Speed", right.getSpeed());
		left.setPID(SmartDashboard.getNumber("DriveTrain LP", left.getP()),
				SmartDashboard.getNumber("DriveTrain LI", left.getI()),
				SmartDashboard.getNumber("DriveTrain LD", left.getD()));
		right.setPID(SmartDashboard.getNumber("DriveTrain RP", right.getP()),
				SmartDashboard.getNumber("DriveTrain RI", right.getI()),
				SmartDashboard.getNumber("DriveTrain RD", right.getD()));
		SmartDashboard.putNumber("Left Distance", left.getNetDistance());
		SmartDashboard.putNumber("Right Distance", right.getNetDistance());
	}

	//enables PID
	public void enablePID() {
		left.enablePID();
		right.enablePID();
	}

	//sets up setpoint
	public void setSetpoint(double targetSpeed) {
		left.setSetpoint(targetSpeed);
		right.setSetpoint(targetSpeed);
	}

	//disables PID
	public void disablePID() {
		left.disablePID();
		right.disablePID();
	}

	//enables Ramping
	public void enableRamping() {
		left.enableRamping();
		right.enableRamping();
	}

	//disables ramping
	public void disableRamping() {
		left.disableRamping();
		right.disableRamping();
	}

	//determines if robot is at target
	public boolean isAtTarget() {
		return left.isAtTarget() && right.isAtTarget();
	}

	//turns on the LED lights
	public void turnOnGlow() {
		glowRight.set(true);
		glowLeft.set(true);
	}

	public void turnOffGlow() {
		glowRight.set(false);
		glowLeft.set(false);
	}

	public void runPID() {
		left.runPID();
		right.runPID();
	}

	//sets up constants for DriveSide
	class DriveSide {
		CANTalon cimOne, cimTwo, miniCim;
		double kP, kI, kD;
		double targetDistance;
		boolean pidEnabled;
		double integralError;
		double previousError;
		double netDistance;
		double time;
		boolean inverted;

		//sets up the control modes for the talons
		public DriveSide(int cimOneID, int cimTwoID, int miniCimID, boolean inverted) {
			cimOne = new CANTalon(cimOneID);
			cimTwo = new CANTalon(cimTwoID);
			miniCim = new CANTalon(miniCimID);
			cimTwo.setFeedbackDevice(FeedbackDevice.QuadEncoder);
			// cimTwo.setProfile(0);
			cimTwo.setPIDSourceType(PIDSourceType.kDisplacement);
			cimTwo.setVoltageRampRate(18);
			cimTwo.changeControlMode(TalonControlMode.Voltage);
			cimOne.changeControlMode(TalonControlMode.Follower);
			miniCim.changeControlMode(TalonControlMode.Follower);
			cimOne.set(cimTwoID);
			miniCim.set(cimTwoID);
			cimOne.setInverted(inverted);
			cimTwo.setInverted(inverted);
			miniCim.setInverted(inverted);
			this.inverted = inverted;
		}

		//enables ramping
		public void enableRamping() {
			cimTwo.setVoltageRampRate(24);
		}

		//disables ramping
		public void disableRamping() {
			cimTwo.setVoltageRampRate(0);
		}

		//gets P
		public double getP() {
			return kP;
		}

		//gets I
		public double getI() {
			return kI;
		}

		//gets D
		public double getD() {
			return kD;
		}

		//returns the speed of cimtwo
		public double getSpeed() {
			return cimTwo.getSpeed() * .04295 * (inverted ? 1 : -(4.0/3.0)); //Remove when encoder repaired
		}

		//sets speed of cimTwo
		public void set(double speed) {
			speed *= 12.0;
			cimTwo.set(speed);
		}

		//sets kP, kI, and kD
		public void setPID(double p, double i, double d) {
			kP = p;
			kI = i;
			kD = d;
		}

		//sets the target distance
		public void setSetpoint(double targetDistance) {
			this.targetDistance = targetDistance;
			cimTwo.setSetpoint(targetDistance);
		}

		// enable PID and clears target distance
		public void enablePID() {
			pidEnabled = true;
			time = System.currentTimeMillis();
			targetDistance = 0;
			integralError = 0;
			previousError = 0;
			netDistance = 0;
			time = 0;
		}

		// disables the PID
		public void disablePID() {
			pidEnabled = false;
		}

		//runs the PID
		public void runPID() {
			if (pidEnabled) {
				double currentDistance = getNetDistance();
				double currentError = (targetDistance - currentDistance);
				integralError += currentError;
				// Motor Voltage = Kp*error + Ki*error_sum +
				// Kd*(error-error_last)
				double speed = kP * currentError + kI * integralError + kD * (currentError - previousError);
				previousError = currentError;
				if (inverted) {
					SmartDashboard.putNumber("Drive left", speed);
					System.out.println("Left Speed" + speed);
				} else {
					SmartDashboard.putNumber("Drive right", speed);
					System.out.println("Right Speed" + speed);
				}
				set(speed);

			} else {
				set(0);
			}
		}

		public boolean isAtTarget() {
			// TODO: Verify grace distance
			return Math.abs(this.targetDistance - getNetDistance()) < .1;
		}

		public double getNetDistance() {
			double pTime = time;
			time = System.currentTimeMillis();
			netDistance += (getSpeed() * (time - pTime)) / 1000.0;
			return netDistance;
		}
	}
}
