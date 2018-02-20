package app;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;


public class MyController {
	@FXML
	private ImageView iv_video;
	@FXML
	private ImageView iv_frame;
	@FXML
	private Button btn_shot;
	@FXML
	private LineChart lc_abs;
	@FXML
	private LineChart lc_relative;
	@FXML
	private TextArea ta_details;
	
	
	private VideoCapture capture;
	private static int cameraId = 0;
	private Mat currentFrame;
	private boolean isInited = false;

	private ScheduledExecutorService timer;

	public void cameraInit(){
		this.capture = new VideoCapture();
		this.capture.open(cameraId);
		startCamera();
		btn_shot.setText("Take a shot");
		isInited = true;
	}
	
	protected void startCamera(){
		if (this.capture.isOpened()){
			Runnable frameGrabber = new Runnable(){
				@Override
				public void run(){							
					updateImageView(iv_video, matToImage(grabFrame()));
				}
			};
			
			this.timer = Executors.newSingleThreadScheduledExecutor();
			this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
		}
	}
	
	@FXML
	protected void takeShot(ActionEvent event){
		if(!isInited) 
			cameraInit();		
		else
		{
			updateImageView(iv_frame, matToImageShot(getFrame(true)));
			getMatrix(grabFrame());
		}
		
	
		
	}

	private Mat grabFrame(){
		Mat frame = new Mat();
		if (this.capture.isOpened()){
			try{
				this.capture.read(frame);
			}catch (Exception e){
				System.err.println("Exception during the image grabing: " + e);
			}
		}
		return frame;
	}
	
	public Mat getFrame(boolean isUpdate){
		if(isUpdate)
			currentFrame = grabFrame();
		return currentFrame;
	}
	
	public static Image matToImage(Mat frame){
		try{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}catch (Exception e){
			System.err.println("Can't convert the Mat to Image: " + e);
			return null;
		}
	}
	public static Image matToImageShot(Mat frame){
		try{
			Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}catch (Exception e){
			System.err.println("Can't convert the Mat to Image: " + e);
			return null;
		}
	}
	
	
	public static BufferedImage matToBufferedImage(Mat original){
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if (original.channels() > 1){
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}else{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}
	
	void updateImageView(ImageView view, Image image){
		Platform.runLater(() -> {
			view.imageProperty().set(image);
		});
	}
	
	void getMatrix(Mat frame) {
		BufferedImage image = matToBufferedImage(frame);
		int w = (int) image.getWidth();
		int h = (int) image.getHeight();

		int[][] array = new int[w][h];
		
		Raster raster =  image.getData();
		for (int j = 0; j < w; j++) {
		    for (int k = 0; k < h; k++) {
		        array[j][k] = raster.getSample(j, k, 0);
		    }
		}
		
		
		
		for (int j = 0; j < w; j++) {
		    for (int k = 0; k < h; k++) {
		        System.out.print(array[j][k]+" "); 
		    }
		    System.out.println("");
		}
		
	}
	
}
