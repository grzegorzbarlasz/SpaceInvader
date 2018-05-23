package com.example.grzesiu.spaceinvaders;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    private Button button;
    private Button button2;

    MediaPlayer bckgrndmusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        bckgrndmusic = MediaPlayer.create(this, R.raw.backgroundmusic);
        bckgrndmusic.start();

        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openSpaceInvadersActivity();
            }
        });

        button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                finish();
                System.exit(0);
            }
        });
    }

    public void openSpaceInvadersActivity(){
        Intent intent = new Intent(MenuActivity.this, SpaceInvadersActivity.class);
        startActivity(intent);

        bckgrndmusic.stop();
    }

}
