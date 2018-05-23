package com.example.grzesiu.spaceinvaders;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class SpaceInvadersView extends SurfaceView implements Runnable {

    Context context;

    private Thread gameThread = null;

    private SurfaceHolder ourHolder;

    private volatile boolean playing;

    private boolean paused = true;

    private Canvas canvas;
    private Paint paint;
    private Paint textPaint;

    private long fps;

    //pomoc do obliczenia fps
    private long timeThisFrame;

    private int screenX;
    private int screenY;

    private PlayerShip playerShip;

    //pocisk gracza
    private Bullet bullet;

    //pociski invadersów
    private Bullet[] invadersBullets = new Bullet[200];
    private int nextBullet;
    private int maxInvaderBullets = 10;

    Invader[] invaders = new Invader[60];
    int numInvaders = 0;

    private DefenceBrick[] bricks = new DefenceBrick[400];
    private int numBricks;

    //dźwięki
    private SoundPool soundPool;
    private int playerExplodeID = -1;
    private int invaderExplodeID = -1;
    private int shootID = -1;
    private int damageShelterID = -1;
    private int uhID = -1;
    private int ohID = -1;

    int score = 0;
    private int lives = 3;

    private long menaceInterval = 1000;

    private boolean uhOrOh;

    private long lastMenaceTime = System.currentTimeMillis();

    public SpaceInvadersView(Context context, int x, int y) {
        super(context);

        this.context = context;

        ourHolder = getHolder();
        paint = new Paint();

        screenX = x;
        screenY = y;

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            descriptor = assetManager.openFd("shoot.ogg");
            shootID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("invaderexplode.ogg");
            invaderExplodeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("damageshelter.ogg");
            damageShelterID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("playerexplode.ogg");
            playerExplodeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("damageshelter.ogg");
            damageShelterID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("uh.ogg");
            uhID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("oh.ogg");
            ohID = soundPool.load(descriptor, 0);

        } catch (IOException e) {
            Log.e("Error: ", "failed to load sound files.");
        }

        prepareLevel();
    }

    private void prepareLevel() {

        playerShip = new PlayerShip(context, screenX, screenY);

        //pociski gracza
        bullet = new Bullet(screenY);

        //inicjalizacja pociskow invadersów
        for(int i = 0; i < invadersBullets.length; i++){
            invadersBullets[i] = new Bullet(screenY);
        }

        //armia invadersów
        numInvaders = 0;
        for(int column = 0; column < 6; column ++ ){
            for(int row = 0; row < 5; row ++ ){
                invaders[numInvaders] = new Invader(context, row, column, screenX, screenY);
                numInvaders ++;
            }
        }

        //schrony
        numBricks = 0;
        for(int shelterNumber = 0; shelterNumber < 4; shelterNumber++){
            for(int column = 0; column < 10; column ++ ) {
                for (int row = 0; row < 5; row++) {
                    bricks[numBricks] = new DefenceBrick(row, column, shelterNumber, screenX, screenY);
                    numBricks++;
                }
            }
        }

        menaceInterval = 1000;

    }

    @Override
    public void run() {
        while (playing) {

            long startFrameTime = System.currentTimeMillis();

            if (!paused) {
                update();
            }

            draw();

            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }

            if(!paused) {
                if ((startFrameTime - lastMenaceTime) > menaceInterval) {
                    if (uhOrOh) {
                        soundPool.play(uhID, 1, 1, 0, 0, 1);

                    } else {
                        soundPool.play(ohID, 1, 1, 0, 0, 1);
                    }

                    lastMenaceTime = System.currentTimeMillis();
                    //zmiana wartości uhOrOh
                    uhOrOh = !uhOrOh;
                }
            }

        }

    }

        private void update() {

            //uderzenie w bok ekranu przez invadera
            boolean bumped = false;

            //gracz przegrał
            boolean lost = false;

            //przesunięcie statku gracza
            playerShip.update(fps);

            //zaktualizowanie invaderów, jeśli są widoczni
            for(int i = 0; i < numInvaders; i++){

                if(invaders[i].getVisibility()) {
                    //przesuń następnego invadersa
                    invaders[i].update(fps);

                    //czy invaders chce strzelić?
                    if(invaders[i].takeAim(playerShip.getX(),
                            playerShip.getLength())){

                        //jeśli chce, spróbuj wystrzelić kulę
                        if(invadersBullets[nextBullet].shoot(invaders[i].getX()
                                        + invaders[i].getLength() / 2,
                                invaders[i].getY(), bullet.DOWN)) {

                            nextBullet++;

                            if (nextBullet == maxInvaderBullets) {
                                //wstrzymanie wystrzelenia kolejnego pocisku, aż do końca podróży
                                //sesja zwraca false, jeśli pocisk jest aktywny
                                nextBullet = 0;
                            }
                        }
                    }

                    //zmiana kierunku poruszania
                    if (invaders[i].getX() > screenX - invaders[i].getLength()
                            || invaders[i].getX() < 0){

                        bumped = true;

                    }
                }

            }

            //aktualizacja pocisków invadersów, jeśli są aktywni
            for(int i = 0; i < invadersBullets.length; i++){
                if(invadersBullets[i].getStatus()) {
                    invadersBullets[i].update(fps);
                }
            }

            if(bumped){

                //przeniesienie wszystkich invadersów i zmiana kierunku
                for(int i = 0; i < numInvaders; i++){
                    invaders[i].dropDownAndReverse();
                    //invadersi wylądowali
                    if(invaders[i].getY() > screenY - screenY / 10){
                        lost = true;
                    }
                }

                //zwiększenie częstotliwości dźwięków
                menaceInterval = menaceInterval - 80;
            }

            if (lost) {
                prepareLevel();
            }

            //aktualizacja pocisków gracza
            if(bullet.getStatus()){
                bullet.update(fps);
            }

            //pocisk gracza uderzył w górną część ekranu
            if(bullet.getImpactPointY() < 0){
                bullet.setInactive();
            }

            //pocisk invadersa urzedył w dolną część ekranu
            for(int i = 0; i < invadersBullets.length; i++){
                if(invadersBullets[i].getImpactPointY() > screenY){
                    invadersBullets[i].setInactive();
                }
            }

            //pocisk gracza trafił invadersa
            if(bullet.getStatus()) {
                for (int i = 0; i < numInvaders; i++) {
                    if (invaders[i].getVisibility()) {
                        if (RectF.intersects(bullet.getRect(), invaders[i].getRect())) {
                            invaders[i].setInvisible();
                            soundPool.play(invaderExplodeID, 1, 1, 0, 0, 1);
                            bullet.setInactive();
                            score = score + 10;

                            //gracz zwyciężył - zacznij od nowa
                            if(score == numInvaders * 10){
                                paused = true;
                                score = 0;
                                lives = 3;
                                prepareLevel();
                            }
                        }
                    }
                }
            }

            //pocisk invadersa trafił w schron
            for(int i = 0; i < invadersBullets.length; i++){
                if(invadersBullets[i].getStatus()){
                    for(int j = 0; j < numBricks; j++){
                        if(bricks[j].getVisibility()){
                            if(RectF.intersects(invadersBullets[i].getRect(), bricks[j].getRect())){
                                //wykrycie kolizji
                                invadersBullets[i].setInactive();
                                bricks[j].setInvisible();
                                soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                            }
                        }
                    }
                }

            }

            //pocisk gracza trafił w schron
            if(bullet.getStatus()){
                for(int i = 0; i < numBricks; i++){
                    if(bricks[i].getVisibility()){
                        if(RectF.intersects(bullet.getRect(), bricks[i].getRect())){
                            //wykrycie kolizji
                            bullet.setInactive();
                            bricks[i].setInvisible();
                            soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                        }
                    }
                }
            }

            //pocisk invadersa trafił w statek gracza
            for(int i = 0; i < invadersBullets.length; i++){
                if(invadersBullets[i].getStatus()){
                    if(RectF.intersects(playerShip.getRect(), invadersBullets[i].getRect())){
                        invadersBullets[i].setInactive();
                        lives--;
                        soundPool.play(playerExplodeID, 1, 1, 0, 0, 1);

                        //koniec gry - zacznij od nowa
                        if(lives == 0){
                            paused = true;
                            lives = 3;
                            score = 0;
                            prepareLevel();
                        }
                    }
                }
            }

        }

    private void draw() {

        if (ourHolder.getSurface().isValid()) {

            canvas = ourHolder.lockCanvas();

            //tło
            Drawable d  = getResources().getDrawable(R.drawable.spacebckgrnd);
            d.setBounds(getLeft(), getTop(), getRight(), getBottom());
            d.draw(canvas);

            //kolor pocisków i schronu
            paint.setColor(Color.argb(255, 255, 255, 255));

            //rysuj gracza
            canvas.drawBitmap(playerShip.getBitmap(), playerShip.getX(), screenY - playerShip.getHeight(), paint);

            //rusyj invadersów
            for(int i = 0; i < numInvaders; i++){
                if(invaders[i].getVisibility()) {
                    if(uhOrOh) {
                        canvas.drawBitmap(invaders[i].getBitmap(), invaders[i].getX(), invaders[i].getY(), paint);
                    }else{
                        canvas.drawBitmap(invaders[i].getBitmap2(), invaders[i].getX(), invaders[i].getY(), paint);
                    }
                }
            }

            //rysuj częśći schronu
            for(int i = 0; i < numBricks; i++){
                if(bricks[i].getVisibility()) {
                    canvas.drawRect(bricks[i].getRect(), paint);
                }
            }

            //rysuj pocisk gracza, jeśli aktywny
            if(bullet.getStatus()){
                canvas.drawRect(bullet.getRect(), paint);
            }

            //rysuj pociski invadersów jeśli aktywne
            for(int i = 0; i < invadersBullets.length; i++){
                if(invadersBullets[i].getStatus()) {
                    canvas.drawRect(invadersBullets[i].getRect(), paint);
                }
            }

            //rysowanie punktów i żyć
            paint.setColor(Color.argb(255, 255, 35, 35));
            paint.setTextSize(screenY/13);
            canvas.drawText("Score: " + score + "   Lives: " + lives, 10, 50, paint);

            //rysowanie wszystkiego na ekran
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    //zamknij wątek, jeśli gra została wstrzymana/zatrzymana
    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }

    }

    //rozpocznij pracę wątka, jeśli uruchomiono activity gry
    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    //wykrywanie dotknięć ekranu
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            //gracz dotknął ekranu
            case MotionEvent.ACTION_DOWN:

                paused = false;

                if(motionEvent.getY() > screenY - screenY / 8) {
                    if (motionEvent.getX() > screenX / 2) {
                        playerShip.setMovementState(playerShip.RIGHT);
                    } else {
                        playerShip.setMovementState(playerShip.LEFT);
                    }

                }

                if(motionEvent.getY() < screenY - screenY / 8) {
                    //wystrzelenie pocisku
                    if(bullet.shoot(playerShip.getX()+
                            playerShip.getLength()/2,screenY,bullet.UP)){
                        soundPool.play(shootID, 1, 1, 0, 0, 1);
                    }
                }
                break;

            //gracz przestał dotykać ekran
            case MotionEvent.ACTION_UP:

                if(motionEvent.getY() > screenY - screenY / 10) {
                    playerShip.setMovementState(playerShip.STOPPED);
                }

                break;

        }

        return true;
    }

}