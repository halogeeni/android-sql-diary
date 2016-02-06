package com.arasio.tictactoe;

import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity {

	private Integer[][] boardMatrix;
	
	// game ends when click count == 9
	private int clickCount;
	private boolean gameWon;
	
	// 1 = human, 4 = cpu
	private int valueOfPlayerInTurn;
	
	final String tieGame = "Tie!";
	final String playerWins = "You win!";
	final String cpuWins = "Computer wins!";
	
	private TableLayout table;
	private TextView gameOutcomeMessage;
	private SharedPreferences gameSettings;
	private SharedPreferences savedGameState;
	private Intent mediaPlayerIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// set preferences to defaults, FOR DEBUGGING ONLY
		//PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		// play music if it's enabled in the settings
		gameSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		if(gameSettings.getBoolean("background_music_preference", true)) {
			mediaPlayerIntent = new Intent(MainActivity.this, GameMusicService.class);
			startService(mediaPlayerIntent);
		}
		
		// initialize new board
		boardMatrix = new Integer[][] {
				{ 0, 0, 0 } , 
				{ 0, 0, 0 } , 
				{ 0, 0, 0 } };
		
		table = (TableLayout)findViewById(R.id.board);
		table.removeAllViews();
		restoreGameState();
		drawBoard(table);
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveGameState();
		// mute the music if it's enabled
		if(gameSettings.getBoolean("background_music_preference", true)) {
			stopService(mediaPlayerIntent);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		restoreGameState();
		table.removeAllViews();
		// start music playback if it's not muted
		if(gameSettings.getBoolean("background_music_preference", true)) {
			mediaPlayerIntent = new Intent(MainActivity.this, GameMusicService.class);
			startService(mediaPlayerIntent);
		}
		// start a new game if click count was 0 or 9, or the game was won 
		// (game was finished or not started at all)
		if(clickCount == 0 || clickCount == 9 || gameWon == true) {
			initializeNewGame();
		} else {
			// continue with human player's turn
			valueOfPlayerInTurn = 1;
		}
		drawBoard(table);
	}
	
	private void saveGameState() {
		savedGameState = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = savedGameState.edit();
		String matrixCoordinate;
		
		// store the board state
		for(int y = 0; y < 3; y++) {
			for(int x = 0; x < 3; x++) {
				matrixCoordinate = "matrixCoordinate_" + x + "_" + y;
				editor.putInt(matrixCoordinate, boardMatrix[x][y]);
			}
		}
		// store click count and game won-flag
		editor.putInt("clickCount", clickCount);
		editor.putBoolean("gameWon", gameWon);
		// commit the edits so they are stored
		editor.commit();
	}
	
	private void restoreGameState() {
		savedGameState = getPreferences(MODE_PRIVATE);
		String matrixCoordinate;
		
		for(int y = 0; y < 3; y++) {
			for(int x = 0; x < 3; x++) {
				matrixCoordinate = "matrixCoordinate_" + x + "_" + y;
				boardMatrix[x][y] = savedGameState.getInt(matrixCoordinate, 0);
			}
		}
		clickCount = savedGameState.getInt("clickCount", 0);
		gameWon = savedGameState.getBoolean("gameWon", false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_activity_actions, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch(item.getItemId()) {
			case R.id.action_newgame:
				table.removeAllViews();
				initializeNewGame();
				drawBoard(table);
				return true;

			case R.id.action_settings:
				// start settings activity
				Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
				
				// mute the music if it's enabled
				if(gameSettings.getBoolean("background_music_preference", true)) {
					stopService(mediaPlayerIntent);
				}
				startActivity(settingsIntent);
				return true;
			
			default:
				return super.onOptionsItemSelected(item);
		}
		
	}

	private void drawBoard(final TableLayout table) {
		int buttonCount = 0;
		Button button;
		TableRow row;
		
		TableLayout.LayoutParams params = 
				new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.MATCH_PARENT);
		TableRow.LayoutParams rowParams = 
				new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.WRAP_CONTENT);
		
		// get user preferences
		gameSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String playerColor = gameSettings.getString("player_color_preference", "#378B2E");
		String cpuColor = gameSettings.getString("cpu_color_preference", "#AA9739");
		String playerSymbol = gameSettings.getString("player_symbol_preference", "X");
		
		// set CPU tile symbol based on player's selection
		String cpuSymbol;
		if(playerSymbol.contentEquals("X")) {
			cpuSymbol = "O";
		} else {
			cpuSymbol = "X";
		}
		
		// button click listener
		OnClickListener tileClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				// game logic
				clickTile( (Integer) v.getTag() );
				valueOfPlayerInTurn = 4;
				
				// make sure a tie game is possible and avoid an infinite loop
				if(clickCount < 9) {
					processCPUTurn();
				}
				
				valueOfPlayerInTurn = 1;
				table.removeAllViews();
				
				if(checkWinner() != 0) {
					gameWon = true;
				}

				if(clickCount == 9 || gameWon == true) {
					gameOutcomeMessage = new TextView(table.getContext());
					
					switch(checkWinner()) {
						case 0:
							gameOutcomeMessage.setText(tieGame);
							break;
						case 1:
							gameOutcomeMessage.setText(playerWins);
							break;
						case 2:
							gameOutcomeMessage.setText(cpuWins);
							break;
					}
					
					table.addView(gameOutcomeMessage);
				}
				
				drawBoard(table);
			}
		};
		
		for (int y = 0; y < 3; y++) {
			row = new TableRow(this);
			row.setLayoutParams(params);
			row.setGravity(Gravity.CENTER);
			
			for (int x = 0; x < 3; x++) {
				// game logic:
				// the game lasts for a total of nine clicks (unless a win check is implemented)
				// the board is drawn:
				// if the value at board matrix coordinate equals zero
				// 		-> the position is empty, thus not owned by either player
				//			-> the button is clickable and colored gray
				// if it equals one
				//		-> the position is occupied by human player
				//			-> it is made unclickable and a colored symbol is added (as set in preferences)
				// if it equals four
				//		-> the position is occupied by the cpu player
				//			-> it is made unclickable and a colored symbol is added (as set in preferences)
				
				switch(boardMatrix[x][y]) {
					case 0:
						button = new Button(this);            
						button.setTextColor(Color.GRAY);  
						button.setLayoutParams(rowParams);
						button.setTag(buttonCount);
						button.setOnClickListener(tileClickListener);
						button.setGravity(Gravity.CENTER);
						row.addView(button);
						if(gameWon) {
							button.setEnabled(false);
						}
						buttonCount++;
						break;
						
					case 1:
						button = new Button(this);            
						button.setTextColor(Color.parseColor(playerColor));
						button.setText(playerSymbol);
						button.setLayoutParams(rowParams);
						button.setGravity(Gravity.CENTER);
						button.setTag(buttonCount);
						button.setEnabled(false);
						row.addView(button);
						buttonCount++;
						break;
						
					case 4:
						button = new Button(this);            
						button.setTextColor(Color.parseColor(cpuColor));
						button.setText(cpuSymbol);
						button.setLayoutParams(rowParams);
						button.setGravity(Gravity.CENTER);
						button.setTag(buttonCount);
						button.setEnabled(false);
						row.addView(button);
						buttonCount++;
						break;
				}
			}
			
			table.addView(row);
		}
	}
	
	private void initializeNewGame() {
		// initialize the board matrix
		boardMatrix = new Integer[][] {
				{ 0, 0, 0 } , 
				{ 0, 0, 0 } , 
				{ 0, 0, 0 } };
		
		// reset click count & game won flag
		clickCount = 0;
		gameWon = false;
		
		// human player always starts
		valueOfPlayerInTurn = 1;
	}

	// re-using code from an old game project
	private int getRandomInteger(int rangeStart, int rangeEnd) {
		Random rand = new Random(System.nanoTime() + System.currentTimeMillis());
		int randomInteger = rand.nextInt(rangeStart + rangeEnd);
		
		return randomInteger;
	}
	
	private void processCPUTurn() {
		int cpuTilePick;
		boolean validPick = false;
		
		// highly advanced AI
		// (a.k.a. unoptimized brute forcing, but does the job at this number range)
		while(!validPick) {
			cpuTilePick = getRandomInteger(0,8);
			if(returnTileValue(cpuTilePick) == 0) {
				clickTile(cpuTilePick);
				validPick = true;
			}
		}
	}
	
	private int returnTileValue(int tileNumber) {
		int tileValue = 0;
		
		switch (tileNumber) {
			case 0:
				tileValue = boardMatrix[0][0];
				break;
			case 1:
				tileValue = boardMatrix[1][0];
				break;
			case 2:
				tileValue = boardMatrix[2][0];
				break;
			case 3:
				tileValue = boardMatrix[0][1];
				break;
			case 4:
				tileValue = boardMatrix[1][1];
				break;
			case 5:
				tileValue = boardMatrix[2][1];
				break;
			case 6:
				tileValue = boardMatrix[0][2];
				break;
			case 7:
				tileValue = boardMatrix[1][2];
				break;
			case 8:
				tileValue = boardMatrix[2][2];
				break;
		}
		
		return tileValue;
		
	}
	
	private void clickTile(int tileNumber) {
		switch (tileNumber) {
			case 0:
				boardMatrix[0][0] = valueOfPlayerInTurn;
				break;
			case 1:
				boardMatrix[1][0] = valueOfPlayerInTurn;
				break;
			case 2:
				boardMatrix[2][0] = valueOfPlayerInTurn;
				break;
			case 3:
				boardMatrix[0][1] = valueOfPlayerInTurn;
				break;
			case 4:
				boardMatrix[1][1] = valueOfPlayerInTurn;
				break;
			case 5:
				boardMatrix[2][1] = valueOfPlayerInTurn;
				break;
			case 6:
				boardMatrix[0][2] = valueOfPlayerInTurn;
				break;
			case 7:
				boardMatrix[1][2] = valueOfPlayerInTurn;
				break;
			case 8:
				boardMatrix[2][2] = valueOfPlayerInTurn;
				break;
		}
		
		clickCount++;
	}
	
	private int checkWinner() {
		// 0 = draw, 1 = player, 2 = cpu
		int winner = 0;
		
		// victory condition checking
		// 		horizontal lines
		if (    returnTileValue(0) + returnTileValue(1) + returnTileValue(2) == 3 || 
				returnTileValue(3) + returnTileValue(4) + returnTileValue(5) == 3 ||
				returnTileValue(6) + returnTileValue(7) + returnTileValue(8) == 3 ||
		//		vertical lines
				returnTileValue(0) + returnTileValue(3) + returnTileValue(6) == 3 ||
				returnTileValue(1) + returnTileValue(4) + returnTileValue(7) == 3 ||
				returnTileValue(2) + returnTileValue(5) + returnTileValue(8) == 3 ||
		//		diagonal lines
				returnTileValue(0) + returnTileValue(4) + returnTileValue(8) == 3 ||
				returnTileValue(6) + returnTileValue(4) + returnTileValue(2) == 3 )
		{
		//	player wins
			winner = 1;
			
		} else if (
			returnTileValue(0) + returnTileValue(1) + returnTileValue(2) == 12 ||
			returnTileValue(3) + returnTileValue(4) + returnTileValue(5) == 12 ||
			returnTileValue(6) + returnTileValue(7) + returnTileValue(8) == 12 ||
			returnTileValue(0) + returnTileValue(3) + returnTileValue(6) == 12 ||
			returnTileValue(1) + returnTileValue(4) + returnTileValue(7) == 12 ||
			returnTileValue(2) + returnTileValue(5) + returnTileValue(8) == 12 ||
			returnTileValue(0) + returnTileValue(4) + returnTileValue(8) == 12 ||
			returnTileValue(6) + returnTileValue(4) + returnTileValue(2) == 12 )
		{
			// cpu wins
			winner = 2;
			
		} else {
			// tie
			winner = 0;
		}
		
		return winner;
	}
}
