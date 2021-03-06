package org.usfirst.frc.team1747.robot;

import org.usfirst.frc.team1747.robot.commands.AutoShoot;
import org.usfirst.frc.team1747.robot.commands.BallEject;
import org.usfirst.frc.team1747.robot.commands.Climb;
import org.usfirst.frc.team1747.robot.commands.InflateClimber;
import org.usfirst.frc.team1747.robot.commands.IntakeBall;
import org.usfirst.frc.team1747.robot.commands.LowerLift;
import org.usfirst.frc.team1747.robot.commands.LowerScooper;
import org.usfirst.frc.team1747.robot.commands.RaiseLift;
import org.usfirst.frc.team1747.robot.commands.RaiseScooper;
import org.usfirst.frc.team1747.robot.commands.Shoot;
import org.usfirst.frc.team1747.robot.commands.Turn90;
import org.usfirst.frc.team1747.robot.commands.TurnOnFlashlight;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class OI {

	private LogitechController controller;
	private LogitechController auxController;

	public OI() {
		controller = new LogitechController(0);
		auxController = new LogitechController(1);
		controller.getStart().whileHeld(new Shoot());
		controller.getLeftBumper().toggleWhenPressed(new IntakeBall());
		controller.getA().whenPressed(new LowerLift());
		controller.getB().toggleWhenPressed(new BallEject());
		controller.getY().whenPressed(new RaiseLift());
		controller.getRightBumper().whileHeld(new AutoShoot());
		// controller.getRightBumper().whileHeld(new AutoShoot());
		// controller.getBack().whenPressed(new DriveStraight());
		auxController.getX().toggleWhenPressed(new IntakeBall());
		auxController.getA().whenPressed(new LowerLift());
		auxController.getB().toggleWhenPressed(new BallEject());
		auxController.getY().whenPressed(new RaiseLift());
		auxController.getBack().toggleWhenPressed(new TurnOnFlashlight());
		auxController.getLeftTrigger().whileHeld(new InflateClimber());
		auxController.getRightTrigger().whileHeld(new Climb());
		auxController.getRightBumper().whileHeld(new RaiseScooper());
		auxController.getLeftBumper().whileHeld(new LowerScooper());
		SmartDashboard.putData("Turn90", new Turn90());
		// auxController.getRightTrigger().whenPressed(new TurnToAngle());
	}

	public LogitechController getController() {
		return controller;
	}

	public LogitechController getAuxController() {
		return auxController;
	}

}