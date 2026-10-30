//Bibliotecas necessárias para o JavaFX
import javafx.application.Application;//Superclass JavaFx
import javafx.scene.Scene;//Conteúdo de uma janela
import javafx.scene.control.Label;//Texto estático
import javafx.scene.layout.StackPane;//estrutura de layout 
import javafx.stage.Stage;//A própria janela do sistema operativo

//teste básico de funcionamento do ambiente gráfico do JavaFx
public class HelloFX extends Application {
	//método de chamada  start pelo Override
    @Override
	//criação da janela 400X200 pixeis com identificação de dois textos estáticos.
    public void start(Stage stage) {
		//texto estático no centro
        Label label = new Label("Olá, JavaFX!");
        stage.setScene(new Scene(new StackPane(label), 400, 200));
		//texto estático na barra de título da janela
        stage.setTitle("Window Hello JavaFX");
		//mostrar na janela
        stage.show();
    }
//DEFAULT MAIN de todas as aplicações JavaFx de arranque de start(Stage stage)
    public static void main(String[] args) {
        launch(args);
    }
}