package com.internshala.connectfour;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Controller implements Initializable {

	private static  final  int COLUMNS = 7;
	private static  final  int ROWS = 6;
	private static  final  int CIRCLE_DIAMETER = 100;
	private static  final  String discColor1 = "FF0000";
	private static  final  String discColor2 ="FFFF00";
	private static String PLAYER_ONE ;// = "Player ONE";
	private static String PLAYER_TWO ;// = "Player TWO";
	private  boolean isPlayerOneTurn = true;
	private boolean isAllowedToInsert = true; //To prevent multiple entry from one user at a time

	private Disc[][] insertedDiscsArray = new Disc[ROWS][COLUMNS];  //For Structural changes

	@FXML
	public GridPane rootGridPane;
	@FXML
	public Pane insertedDiscPane;
	@FXML
	public Label playerNameLabel;
	@FXML
	public TextField playerOneTextField;
	@FXML
	public TextField playerTwoTextField;
	@FXML
	public Button setNamesButton;



	public void createPlayground(){

		Platform.runLater(() -> setNamesButton.requestFocus());

		Shape rectangleWithHoles = createGameStructuralGrid();
		rootGridPane.add(rectangleWithHoles,0,1);

		List<Rectangle> rectangleList = clickClickableColumns();

		for (Rectangle rectangle: rectangleList)
		{
			rootGridPane.add(rectangle, 0, 1);
		}

		setNamesButton.setOnAction(event -> changeName());

	}

	private void changeName() { //Method to set Player names

		PLAYER_ONE = playerOneTextField.getText();
		PLAYER_TWO = playerTwoTextField.getText();
		playerNameLabel.setText(isPlayerOneTurn? PLAYER_ONE : PLAYER_TWO);

		// An alert box to show user has set the names successfully
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Connect Four");
		alert.setHeaderText(" User Name is Set ");
		alert.setContentText("Start Playing!");
		alert.show();
	}

	private Shape createGameStructuralGrid(){

		Shape rectangleWithHoles = new Rectangle((COLUMNS+1)*CIRCLE_DIAMETER,(ROWS+1)*CIRCLE_DIAMETER);

		for (int row = 0; row < ROWS; row++)
		{
			for (int col = 0; col < COLUMNS; col++)
			{
				Circle circle = new Circle();

				circle.setRadius(CIRCLE_DIAMETER/2);
				circle.setTranslateX(CIRCLE_DIAMETER/2);
				circle.setTranslateY(CIRCLE_DIAMETER/2);
				circle.setSmooth(true);
				circle.setTranslateX(col*(CIRCLE_DIAMETER+5) + 80);
				circle.setTranslateY(row*(CIRCLE_DIAMETER+5) + 80);

				rectangleWithHoles = Shape.subtract(rectangleWithHoles,circle);
			}
		}

		rectangleWithHoles.setFill(Color.WHITE);

		return rectangleWithHoles;
	}

	private List<Rectangle> clickClickableColumns(){

		List<Rectangle> rectangleList = new ArrayList<>();

		for (int col = 0; col < COLUMNS ; col++) {

			Rectangle rectangle = new Rectangle(CIRCLE_DIAMETER, (ROWS + 1) * CIRCLE_DIAMETER);

			rectangle.setFill(Color.TRANSPARENT);
			rectangle.setTranslateX(col*(CIRCLE_DIAMETER+5)+CIRCLE_DIAMETER / 4);

			rectangle.setOnMouseEntered(event -> rectangle.setFill(Color.valueOf("#eeeeee26")));
			rectangle.setOnMouseExited(event -> rectangle.setFill(Color.TRANSPARENT));

			final int column = col;

			rectangle.setOnMouseClicked(event -> {
				if (isAllowedToInsert) {

					isAllowedToInsert = false; //when disc is being dropped then no more disc will be inserted
					insertDisc(new Disc(isPlayerOneTurn), column); //Insert disc to rectangle
				}
			});

			rectangleList.add(rectangle);
		}
		return rectangleList;
	}

	private void insertDisc(Disc disc,int column){

		int row = ROWS-1;

		while (row >= 0){

			if(getDiscIfPresent(row,column) == null)
				break;
			row--;
		}
		if (row < 0)  //if full
			return;

		insertedDiscsArray[row][column]= disc; //for structural changes
		insertedDiscPane.getChildren().add(disc);

		disc.setTranslateX(column*(CIRCLE_DIAMETER+5)+CIRCLE_DIAMETER / 4);

		TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.5),disc);

		translateTransition.setToY(row*(CIRCLE_DIAMETER+5)+CIRCLE_DIAMETER / 4);
		int currentRow = row;
		translateTransition.setOnFinished(event -> {

			isAllowedToInsert = true; //To allow next player to insert the disc
			if(gameEnded(currentRow,column)){

				gameOver();
				return;
			}

			isPlayerOneTurn = !isPlayerOneTurn;
			playerNameLabel.setText(isPlayerOneTurn? PLAYER_ONE:PLAYER_TWO);
		});

		translateTransition.play();

	}

	private boolean gameEnded(int row, int column) {

		//Vertical Points
		List<Point2D> verticalPoints = IntStream.rangeClosed(row-3,row+3)
				                        .mapToObj(r -> new Point2D(r,column))
										.collect(Collectors.toList());
		//Horizontal Points
		List<Point2D> horizontalPoints = IntStream.rangeClosed(column-3,column+3)
										.mapToObj(col -> new Point2D(row,col))
										.collect(Collectors.toList());
		//Diagonal 1 Points
		Point2D startPoint1 = new Point2D(row-3,column+3);
		List<Point2D> diagonal1Points = IntStream.rangeClosed(0,6)
										.mapToObj(i->startPoint1.add(i,-i))
										.collect(Collectors.toList());
		//Diagonal 2 Points
		Point2D startPoint2 = new Point2D(row-3,column-3);
		List<Point2D> diagonal2Points = IntStream.rangeClosed(0,6)
										.mapToObj(i->startPoint2    .add(i,i))
										.collect(Collectors.toList());

		boolean isEnded = (checkCombination(verticalPoints) || checkCombination(horizontalPoints)
						|| checkCombination(diagonal1Points) || checkCombination(diagonal2Points));

		return isEnded;
	}

	private boolean checkCombination(List<Point2D> points) {

		int chain = 0;
		for (Point2D point: points)
		{
			int rowIndexForArray = (int) point.getX();
			int columnIndexForArray = (int) point.getY();

			Disc disc = getDiscIfPresent(rowIndexForArray,columnIndexForArray);

			if(disc != null && disc.isPlayerOneMove == isPlayerOneTurn) {

				chain++;
				if(chain == 4){
					return true;
				}
			}else{
				chain = 0;
			}
		}

		return false;
	}

	private Disc getDiscIfPresent(int row, int column) {        //to prevent ArrayIndexOutOfBoundException

		if( row >= ROWS || row < 0 || column >= COLUMNS || column < 0)
			return  null;

		return  insertedDiscsArray[row][column];

	}

	private void gameOver(){

		String winner = isPlayerOneTurn? PLAYER_ONE:PLAYER_TWO;
		System.out.println("Winner is : " +winner);

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Connect Four");
		alert.setHeaderText("The Winner is : " +winner);
		alert.setContentText("Wan't to play again ? ");

		ButtonType yesBtn = new ButtonType("Yes");
		ButtonType noBtn = new ButtonType("No, Exit");

		alert.getButtonTypes().setAll(yesBtn,noBtn);
		//To resolve IllegalStateException
		Platform.runLater(()->{

			Optional<ButtonType> btnClicked = alert.showAndWait();
			if(btnClicked.isPresent() && btnClicked.get() == yesBtn){

				resetGame();
			}
			else {
				Platform.exit();
				System.exit(0);
			}
		});


	}

	public void resetGame() {

		//Remove all inserted Disc from Pane
		insertedDiscPane.getChildren().clear();

		//Structurally make all elements in insertedDisc[][] array to null
		for (int row = 0; row < insertedDiscsArray.length ; row++) {

			for (int col = 0; col < insertedDiscsArray[row].length; col++) {

				insertedDiscsArray[row][col] = null;
				
			}
			
		}

		isPlayerOneTurn=true; //Let player start a new game
		playerNameLabel.setText(PLAYER_ONE);

		createPlayground(); //prepare new playground



	}

	private static class  Disc extends Circle{

		private  final boolean isPlayerOneMove;

		public Disc(boolean isPlayerOneMove) {

			this.isPlayerOneMove=isPlayerOneMove;

			setRadius(CIRCLE_DIAMETER/2);
			setCenterX(CIRCLE_DIAMETER/2);

			setFill(isPlayerOneMove?Color.valueOf(discColor1):Color.valueOf(discColor2));
			setCenterY(CIRCLE_DIAMETER/2);

		}

	}






	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}

}
