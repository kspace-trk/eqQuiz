//　①　説明文
/*
 * プログラミング演習Ｂ　　　プロジェクト課題用
 *
 * 【　サーバー側プログラム　】
 *
 * ＝＝＝＝＝  クライアントごとの「解答のボタンの順番の正しさ」を判定するプログラム  ＝＝＝＝＝
 * 
 *                                                                    T. Yokoi
 * (動作の解説）
 * １．起動するとJFrameが現れる。
 * ２．スレッドとして起動したMultiCastSender.java（マルチキャスト送信プログラム）は、
 *     解答のため待機しているクライアントに対し、一定時間ごとに繰り返して、
 *     サーバ(自分）のIPアドレスと、解答用のUDP通信用のポート番号を送信する。
 * ３．「開始」ボタンを押すと，マルチキャスト通信によって、クライアントに対して
 * 　　開始合図が送られる（各クライアントでは、合図を受信するとクイズが表示され
 *     解答処理に入る）。
 * ４．開始時刻から「　TIME_LIMIT　」秒の間、クライアントからの解答を待つ
 *    （Timerを使用）。
 * ５．解答を受信すると、「受信時刻」・「クライアントIPアドレス」・「解答文字列」を、
 *    それぞれ配列変数に保存する。
 * ６．TIME_LIMIT 秒が経過したら、受信を中止し、集計処理に移る（正解者表示など）。
 * ７．for文を使ってすべてのクライアントの解答を調べて、結果を表示する。
 *    （なお、配列のデータの順番は、解答を早く受け取った順番でもあることでもある。）
 * ８．表示の状態を維持する。
 */

//　②　インポート文
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import static javafx.application.Application.launch;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.data.general.DefaultPieDataset;

public class Server2020_1a extends Application implements Runnable {

//　③　フィールド変数
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
    //　サーバの情報用変数
    InetAddress ia;
    //
    //　UDP_HOST : サーバのIPアドレス用変数
    String UDP_HOST = "";
    //
    // 解答回収用のバッファのサイズ（受信データの臨時保存用）
    private final static int BUFSIZE = 512;
    //
    //解答制限時間(秒)の設定
    int TIME_LIMIT = 73;
    //
    //マルチキャスト送信部品
    MultiCastSender mcs;
    //
    // 自分をスレッドとして動作させるための部品
    Thread kick;
    //
    Image img[]; // 画像ファイルから読み込んだ画像を入れて使用するためのオブジェクト （今回は配列として用意）
    AudioClip ac[]; // 音声ファイルを入れるためのオブジェクト（今回は配列として用意）
    //
    // startTime:　出題をした瞬間の　時刻（ミリ秒）を記録する変数。
    //　(解答の受信時刻との差の計算で、解答時間を算出するために使用。)
    long startTime = 0;

    //　全解答者数用変数　numberOfAnswers
    int numberOfAnswers = 0;
    //
    //　answerTime[] : 各解答者の解答に要したミリ秒保存用配列（最大100人）。
    int maxMember = 100;  //最大人数の設定
    long[] answerTime = new long[maxMember];
    //
    //　answerName[] : 解答者のホスト名用配列 
    String[] answerHost = new String[maxMember];
    //
    //　answer[] :解答者の解答文字保存用配列
    String[] answer = new String[maxMember];
    //
    // isQuizStarted : クイズが開始したかどうかを表すブーリアン型変数。
    boolean[] StartQuiz;
    //
    // timer : 解答時間制限（TIME_LIMIT秒）を監視するためのタイマー部品。
    Timer timer = new Timer();
    //
    // timeIsUp : 時間切れになったことを知らせるための変数　timeup=true　なら時間切れ。
    boolean timeIsUp = false;
    //
    //　GUIのパネル用部品の宣言。
    BorderPane bp = new BorderPane();
    BorderPane p1 = new BorderPane();
    BorderPane p2 = new BorderPane();
    BorderPane p3 = new BorderPane();
    BorderPane p4 = new BorderPane();
    Label l1 = new Label("【　プログラミング演習１B　プロジェクト サーバ　】");
    Label l2 = new Label();
    Button bt1 = new Button("出題開始");
    TextArea ta = new TextArea();
    Canvas cv = new Canvas(400, 400);
    
    // 
    //　④　コンストラクタ 最初に呼ばれるメソッド
    public Server2020_1a() {
        // このプログラムでは何もしていません．
    }

    //　⑤　開始メソッド　start()
    public void start(Stage stage) {

        ////////////////////////////////
        //
        // 1. 自分のIPアドレス取得など
        //////////////////////////////
        // ホストアドレス、ポート番号の表示
        try {
            ia = InetAddress.getLocalHost();
        } catch (Exception e) {
            System.err.println("getLocalHost Error!!");
        }
        UDP_HOST = ia.getHostAddress();
        l2.setText("　　サーバ  IP：" + UDP_HOST + "　 ポート番号:" + UDP_PORT + "　　");
        //
        //////////////////////////////
        //解答用配列の初期化
        for (int i = 0; i < maxMember; i++) {
            answerHost[i] = "";
            answer[i] = "";
        }
        StartQuiz = new boolean[1];
        StartQuiz[0] = false;

        // 2. GUI画面構築
        //////////////////////////////
        //
        HBox hb = new HBox();
        hb.getChildren().add(l1);
        hb.getChildren().add(l2);
        hb.getChildren().add(bt1);
        hb.setAlignment(Pos.CENTER);
        p1.setCenter(hb);
        //
        ta.setText("<<　ここに経過が表示されます。　>>\n");
        p3.setCenter(ta);

        // 
        cv = new Canvas(400, 200);

        p4.setCenter(cv);
        //
        VBox vb2 = new VBox();
        vb2.getChildren().add(p3);
        vb2.getChildren().add(p4);

        p2.setCenter(vb2);
        bp.setTop(p1);
        bp.setCenter(p2);
        //
        //表示サイズ変更
        Scene sc = new Scene(bp, 750, 700);

        //ステージへの追加
        stage.setScene(sc);

        //ステージの表示
        stage.setX(10);
        stage.setY(10);
        stage.setTitle("Server2020_1a");
        stage.show();

        // 3.　マルチキャストプログラムをスレッドとして起動
        ////////////////////////////////
        // マルチキャスト通信を使って、サーバのIPアドレスと、UDP通信用ポート番号を送る。
        mcs = new MultiCastSender(UDP_HOST, UDP_PORT, StartQuiz);
        //　スレッドとして起動する。
        mcs.start();

        //
        //（ア）用意しておいた画像をimgに読み込み、プログラム中で利用できるようにする。

        // ボタンに画像を貼り付けている
        //※ 同じ画像を複数のボタンには設定できないので注意！！

        //
        // 用意しておいた音声をac[]に読み込み、プログラム中で利用できるようにする。

        // 
        // 4.　開始ボタンへのリスナーの登録
        //「出題開始ボタン」 bt1へのリスナの登録
        // MyEventHandlerクラスの定義は，プログラムの末尾にあります．
        bt1.setOnAction(new MyEventHandler());
        //
        // 5. 自分をスレッドとして起動
        ////////////////////////////////
        //自分用スレッドの準備
        if (kick == null) {
            kick = new Thread(this); // 自分をThreadとして登録し起動。
        }
    }
    //
    //　⑥　run( ) メソッド （開始ボタンのクリックで呼ばれる）
    // クイズのスタートボタンの処理
    public void run() {

        // 1. 開始合図のための処理
        StartQuiz[0] = true;
        ta.appendText("<<　クイズを開始しました　>>\n");

        //　解答開始時の　現在のミリ秒を保存する（後で解答時間の算出に使用する）。
        startTime = System.currentTimeMillis();

        // 2. Timerクラスで解答制限時間を20秒に設定。（終了後にshukei（　）を呼び出す。）
        //================================
        //　解答受信モードへ移行
        //================================
        //　開始後TIME_LIMIT秒で締め切るように　タイマーをセットする。
        timer.schedule(new TimerTask() {

            public void run() {
                //　TIME_LIMIT秒経過したら終了処理をするように設定する（ボタンは利用不可にする）。
                timeIsUp = true;
                ta.appendText("解答を締め切りました\n");
                shukei();  //解答を締め切ったら、さっそく集計作業を実行する。
            }
        }, TIME_LIMIT * 1000);  // TIME_LIMITをミリ秒に変換して設定している。

        // 3. 解答の受信モードに入る。
        /////////////////////////////////////////////////////////////////
        ////////解答受信モード  /////////////////////////////////////////
        /////////////////////////////////////////////////////////////////
        //　解答受信モードに入り、解答を受信したら　解答、時刻、クライアント名を配列に保存
        /*
         * 今回のクイズは、クライアントプログラム内に埋め込まれています。 
         * （サーバから開始合図が送られるまで、クライアント側では表示されません。）
         * 
         * クライアント側に書いたクイズと正解を見ながら、以下の集計処理の流れを見てください。
         */
        fin:
        try {
            //　サーバーソケットを作成する。
            DatagramSocket ds = new DatagramSocket(UDP_PORT);
            // 受信したデータを保持するバッファ（入れ物）を作成する。
            byte buffer[] = new byte[BUFSIZE];
            //　接続要求があれば必要な処理を行い、再び待機状態に入る。
            while (timeIsUp == false) {
                try {
                    // 受信用データグラムパケット（データの小包）を作成する。
                    DatagramPacket dp
                            = new DatagramPacket(buffer, buffer.length);
                    // データを受け取る。
                    ds.receive(dp);
                    // データグラムパケットから、データを文字列として取り出す。
                    String packetData = new String(dp.getData());
                    // でも取り出したデータは，512バイトのままのデータである。
                    //  "211                                    " のような長いデータです。

                    //
                    //
                    //++++++++++++++++++++++++++++++++
                    //　今回は、先頭の３文字だけ取り出したい。
                    //　そこで先頭の３文字を取り出して，解答番号として得ることにする。
                    //++++++++++++++++++++++++++++++++
                    String answerData = packetData.substring(0, 3);
                    //++++++++++++++++++++++++++++++++
                    // これで、answerData　は、"211" になった
                    // 確認するには、下記のコメントを外すといい。
                    System.out.println("受信データ: " + answerData);
                    //
                    //　受信情報の保存
                    // まず受信までの時間　jikan
                    long jikan = System.currentTimeMillis() - startTime;
                    answerTime[numberOfAnswers] = jikan;
                    // クライアントの名前／IPアドレスの取得
                    String clientname = dp.getAddress().getHostName();
                    answerHost[numberOfAnswers] = clientname;
                    //　解答データの保存
                    answer[numberOfAnswers] = answerData;
                    //　解答者数のカウント
                    numberOfAnswers++;
                    //
                    //サーバー画面に一人分受信した旨を表示する。
                    ta.appendText(clientname + "から" + answerData + "を受信しました。\n");
                    //
                    //　もし、制限時間を超えていたら、受信ループから抜ける。
                    if (timeIsUp == true) {
                        break fin;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//　⑦ shukei(　)メソッド　（制限時間経過後に行う集計処理）
    ////////////////////
    //集計作業をする部分　（解答締め切り後の集計作業）
    public void shukei() {
        //　TIME_LIMIT秒経過したので、集計モードに移る。

        //++++++++++++++++++++++++++++++++
        //　このサンプルプログラムでの正解は"211"となっているので、判定に移ります。

        /*
        //++++++++++++++++++++++++++++++++
         正解の解答順序は下記の通りです。
        
            問題１　　2つの言語のうち開発された年が早い順
        　　　　解答候補：　　（１）Java,（２）Objective-C
                正解の順番：
        　　　　　　Objective-C,　　Java
        　　　　　よって、正解の場合の最初の2文字は　"21" 
        
            問題２　　2つの言語のうち、「コンパイラ言語」を選ぶ。
        　　　　解答候補：　　（１）C＃、　（２）Ruby
                正解  C#
        　　　　　　よって、解答文字列の3文字目は　"1"
        
        　　※よって、全問正解である文字列は　"211"　。　
        
         */
        //++++++++++++++++++++++++++++++++
        
        int anstrue0 = 0;
        int anstrue1 = 0;
        int anstrue2 = 0;
        int anstrue3 = 0;

        // 1. 解答データを順番に判定しながら、クライアント情報とともに表示。
        //
        //正解かどうかを判定して表示
        
        ta.appendText("＜＜　解答結果　発表　＞＞\n");
        for (int i = 0; i < numberOfAnswers; i++) {
            ta.appendText("No." + (i + 1) + "　　ホスト名：" + answerHost[i] + "　解答時間：" + answerTime[i] + " [ミリ秒]； 　 ");
            
            int sum = 0;
            for(int j = 0; j < answer[i].length() ; j++){
                char c = answer[i].charAt(j);
                sum += Character.getNumericValue(c);
            }
            if(sum == 0){
                anstrue0++;
                ta.appendText("＜0問正解＞ \n");
            }
            else if(sum == 1){
                anstrue1++;
                ta.appendText("＜1問正解＞ \n");
            }
            else if(sum == 2){
                anstrue2++;
                ta.appendText("＜2問正解＞ \n");
            } 
            else if(sum == 3){
                anstrue3++;
                ta.appendText("＜3問正解＞ \n");
            } 
        }
        
        ta.appendText("全問正解者数：　" + anstrue3 + "名\n");
        ta.appendText("2問正解者数：　" + anstrue2 + "名\n");
        ta.appendText("1問正解者数：　" + anstrue1 + "名\n");
        ta.appendText("0問正解者数：　" + anstrue0 + "名\n");
        

        // 2. グラフィクス画面にグラフ表示（各自で実装を！）
        ////////////////////////////
        
        //
        // jfreechartを用いたグラフウィンドウの生成と表示
        //
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
        DefaultPieDataset dataset = new DefaultPieDataset();
        //++++++++++++++++++++++++++++++++
        dataset.setValue("全問正解者数 :" + anstrue3, anstrue3);
        dataset.setValue("2問正解者数 :" + anstrue2, anstrue2);
        dataset.setValue("1問正解者数 :" + anstrue1, anstrue1);
        dataset.setValue("0問正解者数 :" + anstrue0, anstrue0);
        // create a chart...
        JFreeChart chart = ChartFactory.createPieChart(
                "＜＜解答状況＞＞",
                dataset,
                true, // legend?
                true, // tooltips?
                false // URLs?
        );
        ChartFrame frame = new ChartFrame("＜＜クイズ解答結果！＞＞", chart);
        frame.pack();
        frame.setLocation(10, 750);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        launch(args);
    }

    class MyEventHandler implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            //開始処理
            kick.start(); // 自分内のメソッド　run()　が呼ばれる。
            bt1.setDisable(true); //開始ボタンは1度押したら利用不可にする。
        }
    }
}
