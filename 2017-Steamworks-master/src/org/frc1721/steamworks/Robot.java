
package org.frc1721.steamworks;

import org.frc1721.steamworks.commands.*;
import org.frc1721.steamworks.subsystems.*;


import com.kauailabs.navx.frc.AHRS;


import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.PIDSourceType;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.command.CommandGroup;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.VisionThread;

public class Robot extends IterativeRobot {

	public static OI oi;

	/**
	 * Subsystems List In order to create a new subsystem this list must be
	 * appended. Also note that you must initialize your subsystem in
	 * robotInit()
	 */

	public static ClimberController climber;
	public static LiftController lift;
	public static DriveTrain driveTrain;
	public static CustomRobotDrive robotDrive;
	public static NavxController navController;
	public static LCDController lcdController;
	public static CommandGroup autonomousCommand;
	@SuppressWarnings("rawtypes")
	public static SendableChooser autoChooser;
	public static DistanceController distanceController;
	public static PositionEstimator positionEstimator;
	public static DigitalInput topLimitSwitch;
	public static DigitalInput bottomLimitSwitch;
	public static DigitalInput gearLimitSwitch;
	public static CameraSystem cameraSystem;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	/** Initialize the Drive Train systems **/
	public void robotInit() {

		/** Motor Controllers **/
		RobotMap.dtLeft = new VictorSP(RobotMap.dtlPWM);// .setInverted(true);
		RobotMap.dtRight = new VictorSP(RobotMap.dtrPWM);// .setInverted(true);
		RobotMap.cClimb = new VictorSP(RobotMap.climbPWM);
		RobotMap.lLift = new VictorSP(RobotMap.liftPWM);

		/** Encoders **/
		RobotMap.dtlEnc = new Encoder(RobotMap.dtlEncPA, RobotMap.dtlEncPB, RobotMap.dtrEncL);
		RobotMap.dtrEnc = new Encoder(RobotMap.dtrEncPA, RobotMap.dtrEncPB, RobotMap.dtlEncR);
		RobotMap.dtlEnc.setDistancePerPulse(RobotMap.lDPP);
		RobotMap.dtrEnc.setDistancePerPulse(RobotMap.rDPP);

		/** Network Tables **/
		//RobotMap.cameraTable = NetworkTable.getTable("GRIP/myContourReport");

		/** PID Controllers **/
		RobotMap.dtLeftController = new CustomPIDController(RobotMap.dtP, RobotMap.dtI, RobotMap.dtD, RobotMap.dtF,
				RobotMap.dtlEnc, RobotMap.dtLeft, 0.01);
		RobotMap.dtRightController = new CustomPIDController(RobotMap.dtP, RobotMap.dtI, RobotMap.dtD, RobotMap.dtF,
				RobotMap.dtrEnc, RobotMap.dtRight, 0.01);
		RobotMap.dtLeftController.setPIDSourceType(PIDSourceType.kRate);
		RobotMap.dtRightController.setPIDSourceType(PIDSourceType.kRate);

		/** LCD **/
		RobotMap.lcd = new I2C(I2C.Port.kOnboard, 0x27);
		lcdController = new LCDController();
		lcdController.initLCD(RobotMap.lcd);
		LCDController.print(RobotMap.lcd, "Hello World", 1);

		/** Limit Switch's **/
		topLimitSwitch = new DigitalInput(RobotMap.topLs);
		bottomLimitSwitch = new DigitalInput(RobotMap.bottomLs);
		gearLimitSwitch = new DigitalInput(RobotMap.gearLs);

		/** Gyro and navController **/
		RobotMap.navx = new AHRS(SPI.Port.kMXP, RobotMap.navUpdateHz);
		navController = new NavxController("HeadingController", RobotMap.navP, RobotMap.navI, RobotMap.navD,
				RobotMap.navF, RobotMap.navx, PIDSourceType.kDisplacement);
		navController.setDisplacementRange(-180.0,180.0);
		positionEstimator = new PositionEstimator();

		/** Lift **/
		lift = new LiftController();

		/** Climber **/
		climber = new ClimberController();

		/** Robot Drive **/
		// robotDrive.setInvertedMotor(robotDrive.MotorType.kFrontRight, true);
		robotDrive = new CustomRobotDrive(RobotMap.dtLeft, RobotMap.dtRight, RobotMap.dtLeftController,
				RobotMap.dtRightController, navController);
		// robotDrive.stopMotors();

		/** Drive Train **/
		// Add the drive train last since it depends on robotDrive and
		driveTrain = new DriveTrain(robotDrive, navController);
		driveTrain.setGyroMode(CustomRobotDrive.GyroMode.off);
		driveTrain.setDriveScale(RobotMap.driveRateScale, RobotMap.turnRateScale);

		/** Auto Chooser **/
		// Create a chooser for auto so it can be set from the DS
		autonomousCommand = new TestAuto();
		autoChooser = new SendableChooser();
		autoChooser.addDefault("CrossLineStraight", new AutoCrossLineStraight());
		// center of robot about 2 feet off wall
		autoChooser.addObject("TestVision", new AutoTestVision());
		autoChooser.addObject("AutoGearRight", new AutoDepositGear(1.0));
		autoChooser.addObject("AutoGearLeft", new AutoDepositGear(-1.0));
		autoChooser.addObject("DepositSteam10Red", new AutoDepositSteam(2.0, -9.5, RobotMap.redTeam, true));
		/*

		autoChooser.addObject("Steam10Blue", new AutoDepositSteam(2.0, 10.0, RobotMap.blueTeam, false));
		autoChooser.addObject("Steam15Blue", new AutoDepositSteam(2.0, 15.0, RobotMap.blueTeam, false));
		autoChooser.addObject("DepositSteam10Blue", new AutoDepositSteam(2.0, 9.5, RobotMap.blueTeam, true));
		autoChooser.addObject("DepositSteam15Blue", new AutoDepositSteam(2.0, 13.5, RobotMap.blueTeam, true));
		*/
		SmartDashboard.putData("Auto Chooser", autoChooser);

		/** Live Window **/
		/* Add items to live windows */
		LiveWindow.addSensor("Gyro", "navx", RobotMap.navx);
		LiveWindow.addActuator("LeftRobotDrive", "Victor", RobotMap.dtLeft);
		LiveWindow.addActuator("RightRobotDrive", "Victor", RobotMap.dtRight);
		LiveWindow.addSensor("LeftRobotDrive", "Encoder", RobotMap.dtlEnc);
		LiveWindow.addSensor("RightRobotDrive", "Encoder", RobotMap.dtrEnc);
		LiveWindow.addActuator("LeftRobotDrive", "Controller", RobotMap.dtLeftController);
		LiveWindow.addActuator("RightRobotDrive", "Controller", RobotMap.dtRightController);

		/** Distance Controller **/
		distanceController = new DistanceController("DistanceController", RobotMap.distP, RobotMap.distI,
				RobotMap.distD, driveTrain);

		// new Thread(() -> {
		// UsbCamera camera =
		// CameraServer.getInstance().startAutomaticCapture();
		// camera.setResolution(640, 480);
		//
		// CvSink cvSink = CameraServer.getInstance().getVideo();
		// CvSource outputStream = CameraServer.getInstance().putVideo("Blur",
		// 640, 480);
		//
		// Mat source = new Mat();
		// Mat output = new Mat();
		//
		// while (!Thread.interrupted()) {
		// cvSink.grabFrame(source);
		// Imgproc.cvtColor(source, output, Imgproc.COLOR_BGR2);
		// outputStream.putFrame(output);
		// }
		// }).start();
		cameraSystem = new CameraSystem();

		/** Create the OI **/
		oi = new OI();
	}

	@Override
	public void disabledInit() {
	}

	@Override
	public void disabledPeriodic() {
	}

	@Override
	public void autonomousInit() {
		robotDrive.enablePID();

		/*
		 * Gyro is only reset when the mode changes, so shut the it off then
		 * back on in case auto is started multiple times.
		 */

		RobotMap.navx.zeroYaw();
		positionEstimator.setPosition(RobotMap.xStart, RobotMap.yStart);
		driveTrain.setGyroMode(CustomRobotDrive.GyroMode.off);
		autonomousCommand = (CommandGroup) autoChooser.getSelected();
		// autonomousCommand.addCommands();
		autonomousCommand.start();
	}

	@Override
	public void autonomousPeriodic() {
		Scheduler.getInstance().run();
	}

	@Override
	public void teleopInit() {
		robotDrive.enablePID();
		/*
		 * Gyro is only reset when the mode changes, so shut the it off then
		 * back on in case teleop is started multiple times.
		 */
		driveTrain.setGyroMode(CustomRobotDrive.GyroMode.off);
		driveTrain.setGyroMode(CustomRobotDrive.GyroMode.rate);
	}

	@Override
	public void robotPeriodic() {
		printSmartDashboard();
	}

	@Override
	public void teleopPeriodic() {
		Scheduler.getInstance().run();
		LiveWindow.run();
	}

	@Override
	public void testPeriodic() {
		LiveWindow.run();
	}

	private void printSmartDashboard() {
		// out.printf("'%s.printSmartDashboard()' Worked.\n",
		// this.getClass().getName());

		/** Limit Switch Stuff **/
		SmartDashboard.putBoolean("Gear Limit Switch", gearLimitSwitch.get());
		SmartDashboard.putBoolean("Top Limit Switch", topLimitSwitch.get());
		SmartDashboard.putBoolean("Bottom Limit Switch", bottomLimitSwitch.get());

		/** Navx Stuff **/
		SmartDashboard.putNumber("Yaw", RobotMap.navx.getYaw());
		navController.updateSmartDashboard();
		distanceController.updateSmartDashboard();
		positionEstimator.updateSmartDashboard();
		// SmartDashboard.putNumber("Angle",RobotMap.navx.getAngle());
		// SmartDashboard.putNumber("CompassHeading",RobotMap.navx.getCompassHeading());
		// SmartDashboard.putNumber("Altitude",RobotMap.navx.getAltitude());
		// SmartDashboard.putNumber("DisplacementX",RobotMap.navx.getDisplacementX());
		// SmartDashboard.putNumber("DisplacementY",RobotMap.navx.getDisplacementY());
		// SmartDashboard.putNumber("DisplacementZ",RobotMap.navx.getDisplacementZ());
		// SmartDashboard.putNumber("Roll",RobotMap.navx.getRoll());

		/** Camera Data **/
		cameraSystem.updateSmartDashboard();
        /*
		SmartDashboard.putNumber("area", RobotMap.cameraTable.getNumberArray("area", new double[] { -1 })[0]);
		SmartDashboard.putNumber("centerY", RobotMap.cameraTable.getNumberArray("centerY", new double[] { -1 })[0]);
		SmartDashboard.putNumber("centerX", RobotMap.cameraTable.getNumberArray("centerX", new double[] { -1 })[0]);
		SmartDashboard.putNumber("height", RobotMap.cameraTable.getNumberArray("height", new double[] { -1 })[0]);
		SmartDashboard.putNumber("width", RobotMap.cameraTable.getNumberArray("width", new double[] { -1 })[0]);
		SmartDashboard.putNumber("solidity", RobotMap.cameraTable.getNumberArray("solidity", new double[] { -1 })[0]);
		*/
		/** Controller Stuff **/
		SmartDashboard.putNumber("Joystick One YAxis", OI.jsticks[0].getY());
		SmartDashboard.putNumber("Joystick One Twist", OI.jsticks[0].getTwist());

		SmartDashboard.putNumber("Operator LY Axis", OI.jOp.getRawAxis(RobotMap.gamepadLYaxis));
		SmartDashboard.putNumber("Operator Left Trigger", OI.jOp.getRawAxis(RobotMap.gamepadLTrigger));
		SmartDashboard.putNumber("Operator Right Trigger", OI.jOp.getRawAxis(RobotMap.gamepadRTrigger));

		/** PID Stuff **/
		SmartDashboard.putBoolean("PID", Robot.robotDrive.getPIDStatus());
		

		
	}
}
