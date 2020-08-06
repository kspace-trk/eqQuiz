// ①　説明文
/*
 * プログラミング演習Ｂ　　プロジェクト課題用
 *
 *  ＜＜　クライアント側プログラム　＞＞ 　Client2020_1a.java
 *
 * ＝＝＝＝＝  解答ボタンを押す順番や選択肢を、文字列としてサーバへ送信するプログラム  ＝＝＝＝＝
 *
 *                                                                    T. Yokoi
 * 
 * (動作の解説）
 * １．起動後に、待機用画面が表示される。
 * ２．スレッドとして MultiCastReceiver（マルチキャスト通信の受信プログラム）を起動し、
 *     サーバ情報（IPアドレスとポート番号）を受信するモードに入り、受信したら保存する。
 * ３．さらに、マルチキャスト通信のポートを監視して、開始合図の情報を待つ。
 * ４．サーバ側で、開始ボタンが押されると、マルチキャスト通信で、
 *     開始合図の文字列"StartQuiz!"がクライアントに一斉に送られてくる。
 * ５．開始合図を受けとったら、クイズを表示する状態に切り替える。
 * ６．解答者がボタンをクリックして解答する（配布例では2問からなる）
 *　    1）　問1：　正解と思う順に，ボタンを順番に２つクリックする．
 *　　　2）　問2：　正解と思う方のボタンをクリックする．   
 *      3)   上記の回答をすると，自動的に回答を送信する．　　
 * ７．解答情報（今回は「ボタンの番号を表す数字 3文字」）を，UDP通信によってサーバへ送信する。
 * （８．その後、サーバ側で、結果が集計される。）
 * 　　以上
 */

// ②　インポート文
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import static javafx.application.Application.launch;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import java.util.Random;



/**
 *
 * @author yokoi
 */
public class Client2020_1a extends Application implements Runnable {

// ③　フィールド変数
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //@@@@@@@@    要変更　　   要変更　　   要変更　　 @@@@@@@@
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    ///////////////////////////////////////////////////////////////////////////
    // まず、UDP＿PORTの数字を、自分の学年・学籍番号によって下記の数値に書き換えること。
    // 解答送信用 UDPポート番号を（ 55000 + 学年×1000　＋　学番下３桁）　に設定する。
    // （例）2年生で、学番の下3桁が　123　である学生の場合：
    //       int UDP_PORT = 55000 + 2*1000 + 123;
    int UDP_PORT = 55000 + 3 * 1000 + 67;   
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //
    // 接続先ホスト情報の設定文字列変数
    String[] UDP_HOST = new String[1];
    boolean[] StartQuiz = new boolean[1];
    boolean[] StopQuiz = new boolean[1];
    boolean addedQuizPanel = false;
    String answer = "";    //解答送信用文字列
    //　一度解答を送信したら、二度送りしないために、この変数sentに送信済みであることを記録する。
    boolean sent = false;
    int state = 0;
    int answerNum = 0;
    int chkbt = 0;
    
    //マルチキャスト送信部品
    MultiCastReceiver mcr;

    Thread kick; // 自分をスレッドとして動作させるための部品（ジェットコースターのような乗り物）
    Image img[]; // 画像ファイルから読み込んだ画像を入れて使用するためのオブジェクト （今回は配列として用意）
    AudioClip ac[]; // 音声ファイルを入れるためのオブジェクト（今回は配列として用意）
    ////////////////////////////////////
    //　クライアント画面の構成
    //
    //　土台のパネル
    BorderPane bp = new BorderPane();
    // クイズ用部品を載せるパネル（開始までは不可視にする部品）
    BorderPane bp2 = new BorderPane();
    // 
    //　タイトル用のラベル
    Label l1 = new Label("＜＜　プログラミング演習Ｂ　プロジェクト　クライアント　＞＞");
    //　メッセージ用のラベル
    Label l2 = new Label("　・　開始の合図まで、準備して待機して下さい。");
    // 
    //　クイズ表示用のテキストエリア
    TextArea ta = new TextArea();
    TextField tf = new TextField();
    // 
    //
    Canvas cv = new Canvas(1000, 600);
    
    //　問題開始ボタンを用意。
    Button p1bt = new Button();
    Button p2bt = new Button();
    Button p3bt = new Button();
    Button p4bt = new Button();
    Button p5bt = new Button();
    Button p6bt = new Button();
    Button p7bt = new Button();
    // 
    //クリック回数カウント用変数

// ④　コンストラクタ  (インスタンス生成の最初に実行)
    public Client2020_1a() {
        // サーバ情報の初期化
        UDP_HOST[0] = "";       //  サーバIPアドレス用
        StartQuiz[0] = false;   //  開始合図の有無　（有：true　／　無:false）
    }

//　⑤	開始メソッド　start()
    public void start(Stage stage) {
        //
        // 1. マルチスレッドの起動　（サーバ情報と開始合図の取得用）
        //
        //　サーバからのマルチキャスト通信を受信するためのスレッドを用意する。
        //　　スレッドには、「サーバIPアドレス」と、「開始合図値(true)」 を受信させる。
        mcr = new MultiCastReceiver(UDP_HOST, StartQuiz);
        //　スレッドを起動する。
        mcr.start();

        //
        // 2. 画像、音のファイルの読み込み
        img = new Image[10];
        for (int i = 0; i < 8; i++) {
            // 用意しておいた画像をimgに読み込み、プログラム中で利用できるようにする。
            Path path = Paths.get("src/images/" + i + ".png");
            String imageURL = path.toUri().toString();
            img[i] = new Image(imageURL,1000,600,false,true);
        }
        //
        
        
        //
        // 用意しておいた音声をac[]に読み込み、プログラム中で利用できるようにする。
        ac = new AudioClip[8];
        //
        // 用意しておいた音声をac[]に読み込み、プログラム中で利用できるようにする。
        Path path1 = Paths.get("src/sounds/0.mp3");
        Path path2 = Paths.get("src/sounds/1.mp3");
        Path path3 = Paths.get("src/sounds/2.mp3");
        Path path4 = Paths.get("src/sounds/3.mp3");
        Path path5 = Paths.get("src/sounds/4.mp3");
        Path path6 = Paths.get("src/sounds/5.mp3");
        Path path7 = Paths.get("src/sounds/6.mp3");
        Path path8 = Paths.get("src/sounds/7.mp3");
        String soundURL1 = path1.toUri().toString();
        String soundURL2 = path2.toUri().toString();
        String soundURL3 = path3.toUri().toString();
        String soundURL4 = path4.toUri().toString();
        String soundURL5 = path5.toUri().toString();
        String soundURL6 = path6.toUri().toString();
        String soundURL7 = path7.toUri().toString();
        String soundURL8 = path8.toUri().toString();
        ac[0] = new AudioClip(soundURL1);
        ac[1] = new AudioClip(soundURL2);
        ac[2] = new AudioClip(soundURL3);
        ac[3] = new AudioClip(soundURL4);
        ac[4] = new AudioClip(soundURL5);
        ac[5] = new AudioClip(soundURL6);
        ac[6] = new AudioClip(soundURL7);
        ac[7] = new AudioClip(soundURL8);

        //////////////////////////////////////////////////////
        //
        //　クイズ画面　（開始合図までは表示されない）
        //
        // ①　テキストエリアにクイズを表示する。
        //
        //++++++++++++++++++++++++++++++++
        //　複数問題を出す場合も、第1問の文章は、ここに書いておきます。
        //++++++++++++++++++++++++++++++++
        ta.setFont(new Font(15.0));
        ta.appendText("カットされた周波数帯を当ててください　\n");
        ta.appendText("1～7の数字のどれかをクリックしてください　\n");
        ta.appendText("　※　全部で3問で、フラットな音と、EQかかった音がそれぞれ10秒ずつ流れます．　\n");
        ta.appendText("　　　10秒後に自動で開始します\n\n");
        ta.appendText("　\t\t\t\t出題者: 中尾圭吾");
        

        //
        //　②　問題開始ボタンを配置
        
        p1bt.setText("");
        p2bt.setText("");
        p3bt.setText("");
        p4bt.setText("");
        p5bt.setText("");
        p6bt.setText("");
        p7bt.setText("");
        
        p1bt.setDisable(true);
        p2bt.setDisable(true);
        p3bt.setDisable(true);
        p4bt.setDisable(true);
        p5bt.setDisable(true);
        p6bt.setDisable(true);
        p7bt.setDisable(true);
        
        p1bt.setOpacity(0);
        p2bt.setOpacity(0);
        p3bt.setOpacity(0);
        p4bt.setOpacity(0);
        p5bt.setOpacity(0);
        p6bt.setOpacity(0);
        p7bt.setOpacity(0);
         
        p1bt.setPrefSize(40,40);
        p2bt.setPrefSize(40,40);
        p3bt.setPrefSize(40,40);
        p4bt.setPrefSize(40,40);
        p5bt.setPrefSize(40,40);
        p6bt.setPrefSize(40,40);
        p7bt.setPrefSize(40,40);
        
        ta.setPrefSize(1000,130);
        
        // 
        
        // 3. GUI画面の構築
        /////////////////////////
        // 上側のパネル（タイトルや待機メッセージ）の設定
        VBox vb1 = new VBox();
        vb1.getChildren().add(l1);
        vb1.getChildren().add(l2);

        //　下側のパネル（問題文や、解答用ボタン用）の設定
        VBox vb2 = new VBox();
        vb2.getChildren().add(ta);  //  問題文表示用部品
        vb2.getChildren().add(tf);
        vb2.getChildren().add(cv); //  グラフィックス描画用パネル

        HBox hb2 = new HBox();
        vb2.getChildren().add(hb2);//  解答ボタンを載せるパネル
        //130,220,300,380,460,540
        AnchorPane pf = new AnchorPane(); 
        p1bt.setLayoutX(130);
        p1bt.setLayoutY(430);
        p2bt.setLayoutX(220);
        p2bt.setLayoutY(430);  
        p3bt.setLayoutX(300);
        p3bt.setLayoutY(430);
        p4bt.setLayoutX(380);
        p4bt.setLayoutY(430);
        p5bt.setLayoutX(460);
        p5bt.setLayoutY(430);
        p6bt.setLayoutX(540);
        p6bt.setLayoutY(430);
        p7bt.setLayoutX(620);
        p7bt.setLayoutY(430);
        pf.getChildren().addAll(vb2,p1bt,p2bt,p3bt,p4bt,p5bt,p6bt,p7bt);
        // 開始の合図までは、問題文と解答ボタンの土台パネルを不可視にする。
        //
        bp2.setVisible(false);
        bp2.setCenter(pf);

        bp.setTop(vb1);
        bp.setCenter(bp2);
        //
        //表示サイズ変更
        Scene sc = new Scene(bp, 1000, 800);

        //ステージへの追加
        stage.setScene(sc);

        //ステージの表示
        stage.setX(800);
        stage.setY(10);
        stage.setTitle("Client2020_1a");
        stage.show();
        
        
        
        
        // 4. ボタン1のイベントリスナーの設定
        //
        //　解答ボタンbt1へリスナを登録する。
        p1bt.setOnAction(new MyEventHandler1());
        p2bt.setOnAction(new MyEventHandler2());
        p3bt.setOnAction(new MyEventHandler3());
        p4bt.setOnAction(new MyEventHandler4());
        p5bt.setOnAction(new MyEventHandler5());
        p6bt.setOnAction(new MyEventHandler6());
        p7bt.setOnAction(new MyEventHandler7());
        // 6. スレッドとして、自分を起動
        //　　
        if (kick == null) {
            kick = new Thread(this);
            kick.start();
        }
    }

    // ⑦ run（　）　（開始合図の受信、開始後はアニメーション表示など）
    public void run() {

        
        while (true) {
            // 1. クイズの開始合図の受信判定
            //　クイズ開始の合図を受け取ったら、、クイズ画面を表示する。
            if (StartQuiz[0] == true) {

                // 2. 開始なら、下記を実行。
                // 開始合図を受け取ったら、１回だけクイズ用のパネルp2を見えるようにする。
                if (addedQuizPanel == false) {
                    addedQuizPanel = true;
                    //　bgmを開始する。
                    //
                    bp2.setVisible(true);
                    l2.setVisible(false);
                    GraphicsContext gc = cv.getGraphicsContext2D();
                    gc.drawImage(img[0], 0, 0);
                    for(int i=0;i<100;i++){
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                    }
                    Random rand = new Random();
                    if(state == 0){
                        answerNum = 7;
                        System.out.println(answerNum);
                        ac[0].play();
                        ta.setText("カットされた周波数帯を当ててください　\n");
                        ta.appendText("\n");
                        ta.appendText("1問目　10秒後にイコライザーが適用されます\n\n");
                        gc.drawImage(img[0], 0, 0);
                        
                        
                        for(int i=0;i<100;i++){
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        ac[0].stop();//フラットの音を止める
                        ac[answerNum].play();//EQの音を再生する
                        //全ボタン有効化
                        p1bt.setDisable(false);
                        p2bt.setDisable(false);
                        p3bt.setDisable(false);
                        p4bt.setDisable(false);
                        p5bt.setDisable(false);
                        p6bt.setDisable(false);
                        p7bt.setDisable(false);
                        ta.appendText("イコライザーが適用されています。カットされている周波数の数字を押してください。\n10秒後に次の問題に進みます\n\n");
                        for(int i=0;i<100;i++){
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        ac[answerNum].stop();//EQの音を止める
                        if(chkbt == 0){
                            answer += 0;
                            p1bt.setDisable(true);
                            p2bt.setDisable(true);
                            p3bt.setDisable(true);
                            p4bt.setDisable(true);
                            p5bt.setDisable(true);
                            p6bt.setDisable(true);
                            p7bt.setDisable(true);
                        }
                        
                        state = 1;
                    }
                    if(state == 1){
                        ac[0].play();
                        ta.setText("カットされた周波数帯を当ててください　\n");
                        ta.appendText("\n");
                        ta.appendText("2問目　10秒後にイコライザーが適用されます\n\n");
                        gc.drawImage(img[0], 0, 0);
                        
                        chkbt = 0;
                        

                        answerNum = rand.nextInt(7) + 1;
                        System.out.println(answerNum);
                        
                        for(int i=0;i<100;i++){
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        ac[0].stop();//フラットの音を止める

                        ac[answerNum].play();//EQの音を再生する
                        //全ボタン有効化
                        p1bt.setDisable(false);
                        p2bt.setDisable(false);
                        p3bt.setDisable(false);
                        p4bt.setDisable(false);
                        p5bt.setDisable(false);
                        p6bt.setDisable(false);
                        p7bt.setDisable(false);
                        ta.appendText("イコライザーが適用されています。カットされている周波数の数字を押してください。\n10秒後に次の問題に進みます\n\n");
                        for(int i=0;i<100;i++){
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        ac[answerNum].stop();//EQの音を止める
                        if(chkbt == 0){
                            answer += 0;
                            p1bt.setDisable(true);
                            p2bt.setDisable(true);
                            p3bt.setDisable(true);
                            p4bt.setDisable(true);
                            p5bt.setDisable(true);
                            p6bt.setDisable(true);
                            p7bt.setDisable(true);
                        }

                        state = 2;
                    }
                    if(state == 2){
                        ac[0].play();
                        ta.setText("カットされた周波数帯を当ててください　\n");
                        ta.appendText("\n");
                        ta.appendText("3問目　10秒後にイコライザーが適用されます\n\n");
                        gc.drawImage(img[0], 0, 0);
                        chkbt = 0;
                        
                        answerNum = rand.nextInt(7) + 1;
                        System.out.println(answerNum);
                        
                        for(int i=0;i<100;i++){
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        ac[0].stop();//フラットの音を止める
                        ac[answerNum].play();//EQの音を再生する
                        //全ボタン有効化
                        p1bt.setDisable(false);
                        p2bt.setDisable(false);
                        p3bt.setDisable(false);
                        p4bt.setDisable(false);
                        p5bt.setDisable(false);
                        p6bt.setDisable(false);
                        p7bt.setDisable(false);
                        ta.appendText("イコライザーが適用されています。カットされている周波数の数字を押してください。\n10秒後に次の問題に進みます\n\n");
                        for(int i=0;i<100;i++){
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        ac[answerNum].stop();//EQの音を止める
                        if(chkbt == 0){
                            answer += 0;
                            p1bt.setDisable(true);
                            p2bt.setDisable(true);
                            p3bt.setDisable(true);
                            p4bt.setDisable(true);
                            p5bt.setDisable(true);
                            p6bt.setDisable(true);
                            p7bt.setDisable(true);
                        }

                        state = 3;
                    }

                    if(state == 3){
                        ac[0].stop();
                        ta.setText("何問正解できましたか？\n");
                        ta.appendText("\n");
                        ta.appendText("問題は以上です\n\n");
                    }
                }
                System.out.println("answer: " + answer); // 送信文字列を表示して確認。

                 //　まだ送信したことがなければ、送信する。
                if (sent == false) { // 未送信の場合に限り送信処理を行うための判定
                // 解答をUDPでサーバーへ送信する。
                    try {

                        // １.　宛先のInetAddressを名前から作成する。
                        InetAddress ia = InetAddress.getByName(UDP_HOST[0]);
                        // ２.　宛先のポートを設定する(送信側，受信側で同じポートを指定すること！）。
                        //　　　　　UDP_PORT　（プログラムの冒頭で指定してある）
                        // ３.　データグラムソケットを作成する
                        DatagramSocket ds = new DatagramSocket();
                        // ４.　送りたいメッセージを文字で作成する。
                        String myMessage = answer;
                        // ５　メッセージの文字列をバイトの列に直して，パケット（データの小包）に格納する。
                        byte buffer[] = myMessage.getBytes();
                        DatagramPacket dp = new DatagramPacket(buffer, buffer.length, ia, UDP_PORT);
                        // ６　データグラムパケットを送信する
                        ds.send(dp);
                        // ７　送信がうまくいったら，二度送らないために，送信状態の変数sentをtrueにする。
                        sent = true;
                        // ８　一度送ったらボタンを使用不可にする。
                    
                        // ９　
                        ta.appendText("<< 送信しました．．．． >>");

                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    //　データを送ったら終了
                }
                //
                //　解答送信後に、解答用文字列を初期化する
                answer = "";
                
            }
            // (ウ)	以上を無限ループで継続
            try {
                    Thread.sleep(300);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
    class MyEventHandler1 implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            GraphicsContext gc = cv.getGraphicsContext2D();
            ac[answerNum].stop();
            chkbt = 1;
            //2回押せないようにする
            p1bt.setDisable(true);
            p2bt.setDisable(true);
            p3bt.setDisable(true);
            p4bt.setDisable(true);
            p5bt.setDisable(true);
            p6bt.setDisable(true);
            p7bt.setDisable(true);
            if(answerNum == 1){
                ta.appendText("\t正解");
                gc.drawImage(img[1], 0, 0);
                answer += "1"; 
            }else{
                ta.appendText("\t不正解　正解は" + answerNum + "でした");
                gc.drawImage(img[answerNum], 0, 0);
                answer += "0"; 
            }

        }
    }
    class MyEventHandler2 implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            GraphicsContext gc = cv.getGraphicsContext2D();
            gc.drawImage(img[7], 0, 0);
            ac[answerNum].stop();
            chkbt = 1;
            //2回押せないようにする
            p1bt.setDisable(true);
            p2bt.setDisable(true);
            p3bt.setDisable(true);
            p4bt.setDisable(true);
            p5bt.setDisable(true);
            p6bt.setDisable(true);
            p7bt.setDisable(true);
            if(answerNum == 2){
                ta.appendText("\t正解");
                gc.drawImage(img[2], 0, 0);
                answer += "1"; 
            }else{
                ta.appendText("\t不正解　正解は" + answerNum + "でした");
                gc.drawImage(img[answerNum], 0, 0);
                answer += "0"; 
            }

        }
    }
    class MyEventHandler3 implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            GraphicsContext gc = cv.getGraphicsContext2D();
            gc.drawImage(img[7], 0, 0);
            ac[answerNum].stop();
            chkbt = 1;
            //2回押せないようにする
            p1bt.setDisable(true);
            p2bt.setDisable(true);
            p3bt.setDisable(true);
            p4bt.setDisable(true);
            p5bt.setDisable(true);
            p6bt.setDisable(true);
            p7bt.setDisable(true);
            if(answerNum == 3){
                ta.appendText("\t正解");
                gc.drawImage(img[3], 0, 0);
                answer += "1";
            }else{
                ta.appendText("\t不正解　正解は" + answerNum + "でした");
                gc.drawImage(img[answerNum], 0, 0);
                answer += "0"; 
            }

        }
    }
    class MyEventHandler4 implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            GraphicsContext gc = cv.getGraphicsContext2D();
            gc.drawImage(img[7], 0, 0);
            ac[answerNum].stop();
            chkbt = 1;
            //2回押せないようにする
            p1bt.setDisable(true);
            p2bt.setDisable(true);
            p3bt.setDisable(true);
            p4bt.setDisable(true);
            p5bt.setDisable(true);
            p6bt.setDisable(true);
            p7bt.setDisable(true);
            if(answerNum == 4){
                ta.appendText("\t正解");
                gc.drawImage(img[4], 0, 0);
                answer += "1"; 
            }else{
                ta.appendText("\t不正解　正解は" + answerNum + "でした");
                gc.drawImage(img[answerNum], 0, 0);
                answer += "0"; 
            }

        }
    }
    class MyEventHandler5 implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            GraphicsContext gc = cv.getGraphicsContext2D();
            gc.drawImage(img[7], 0, 0);
            ac[answerNum].stop();
            chkbt = 1;
            //2回押せないようにする
            p1bt.setDisable(true);
            p2bt.setDisable(true);
            p3bt.setDisable(true);
            p4bt.setDisable(true);
            p5bt.setDisable(true);
            p6bt.setDisable(true);
            p7bt.setDisable(true);
            if(answerNum == 5){
                ta.appendText("\t正解");
                gc.drawImage(img[5], 0, 0);
                answer += "1"; 
            }else{
                ta.appendText("\t不正解　正解は" + answerNum + "でした");
                gc.drawImage(img[answerNum], 0, 0);
                answer += "0"; 
            }

        }
    }
    class MyEventHandler6 implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            GraphicsContext gc = cv.getGraphicsContext2D();
            gc.drawImage(img[7], 0, 0);
            ac[answerNum].stop();
            chkbt = 1;
            //2回押せないようにする
            p1bt.setDisable(true);
            p2bt.setDisable(true);
            p3bt.setDisable(true);
            p4bt.setDisable(true);
            p5bt.setDisable(true);
            p6bt.setDisable(true);
            p7bt.setDisable(true);
            if(answerNum == 6){
                ta.appendText("\t正解");
                gc.drawImage(img[6], 0, 0);
                answer += "1"; 
            }else{
                ta.appendText("\t不正解　正解は" + answerNum + "でした");
                gc.drawImage(img[answerNum], 0, 0);
                answer += "0"; 
            }

        }
    }
    class MyEventHandler7 implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            GraphicsContext gc = cv.getGraphicsContext2D();
            gc.drawImage(img[7], 0, 0);
            ac[answerNum].stop();
            chkbt = 1;
            //2回押せないようにする
            p1bt.setDisable(true);
            p2bt.setDisable(true);
            p3bt.setDisable(true);
            p4bt.setDisable(true);
            p5bt.setDisable(true);
            p6bt.setDisable(true);
            p7bt.setDisable(true);
            if(answerNum == 7){
                ta.appendText("\t正解");
                gc.drawImage(img[7], 0, 0);
                answer += "1"; 
            }else{
                ta.appendText("\t不正解　正解は" + answerNum + "でした");
                gc.drawImage(img[answerNum], 0, 0);
                answer += "0"; 
            }

        }
    }
    
}
