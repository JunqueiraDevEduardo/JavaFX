
// AppTarefas.java  -  Camada 1 (Frontend): o cliente em ambiente gráfico no JavaFX

// Esta classe é a aplicação que o utilizador/administrador vê: a janela de login e, depois
// de entrar, o ecrã principal com a tabela de tarefas. O cliente nunca comunica com a base de dados. Só envia pedidos
// HTTP ao servidor (Tomcat), através da classe Api. Quem decide o que cada
// utilizador pode fazer é o SERVIDOR TOMCAT; 
//
// O que cada papel pode fazer (e o método HTTP usado em cada operação):
//
//   Operação                      UTILIZADOR            ADMINISTRADOR
//  
//   GET    listar tarefas         só as suas            todas
//   POST   criar tarefa           para si               para si
//   PUT    alterar tarefa         só as suas            qualquer uma
//   DELETE apagar tarefa          NÃO PODE (403)        qualquer uma
//   GET    listar utilizadores    NÃO PODE (403)        sim
//   POST   criar utilizador       NÃO PODE (403)        sim
//   DELETE apagar utilizador      não existe (405)      não existe (405)

// ---------- Bibliotecas base: a aplicação, a janela e o ecrã ----------
import javafx.application.Application;   // classe-mãe de toda a aplicação JavaFX
import javafx.application.Platform;      // permite voltar, de execução do ecrã (runLater)
import javafx.stage.Stage;               // Stage = a JANELA 
import javafx.scene.Scene;               // Scene = o ECRÃ (o conteúdo que fica dentro da janela)
import javafx.scene.Node;                // Node = qualquer componente visual (botão, etiqueta, caixa...)

// ---------- Bibliotecas de controlos (os componentes que o utilizador usa) ----------
import javafx.scene.control.Alert;           // caixa de diálogo (usada para confirmar)
import javafx.scene.control.Button;          // botão
import javafx.scene.control.ButtonType;      // botões Sim/Não das caixas de diálogo
import javafx.scene.control.ComboBox;        // lista de escolha (Utilizador / Administrador)
import javafx.scene.control.Label;           // etiqueta de texto
import javafx.scene.control.PasswordField;   // campo de texto que esconde os caracteres
import javafx.scene.control.TableColumn;     // coluna de uma tabela
import javafx.scene.control.TableView;       // tabela
import javafx.scene.control.TextField;       // campo de texto
import javafx.scene.control.TextInputDialog; // caixa de diálogo com um campo de texto (editar título)

// ---------- Bibliotecas de disposição (como os componentes se organizam) ----------
import javafx.scene.layout.HBox;      // organiza os componentes na horizontal
import javafx.scene.layout.Priority;  // define quem ocupa o espaço que sobra
import javafx.scene.layout.Region;    // área vazia, usada como espaçador
import javafx.scene.layout.VBox;      // organiza os componentes na vertical

// ---------- Bibliotecas de propriedades e coleções ----------
import javafx.beans.property.SimpleStringProperty; // valor que a tabela consegue observar
import javafx.collections.FXCollections;           // cria listas observáveis
import javafx.collections.ObservableList;          // lista que avisa a tabela quando muda
import javafx.geometry.Insets;                     // margens interiores
import javafx.geometry.Pos;                        // alinhamentos

// ---------- Bibliotecas de dados do Java ----------
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;  // função que recebe um valor e não devolve nada
import java.util.function.Function;  // função que recebe um valor e devolve outro

// O JSON lido do servidor chega como Object e é convertido (cast) para Map/List.
// O compilador avisaria que essas conversões não são verificadas; esta anotação
// desliga esses avisos em toda a classe.
@SuppressWarnings("unchecked")
public class AppTarefas extends Application {

    // Um "Pedido" é um bocado de código que fala com o servidor e devolve a resposta.
    // Serve para passar esse código ao método executar(), que o corre em segundo plano.
    private interface Pedido {
        Api.Resposta chamar() throws Exception;
    }

    // ---------- Estado partilhado da aplicação ----------

    // O cliente HTTP. Guarda o cookie de sessão (JSESSIONID) entre pedidos.
    private final Api api = new Api();

    //duas listas observáveis
    private final ObservableList<Map<String, Object>> tarefas = FXCollections.observableArrayList();
    private final ObservableList<Map<String, Object>> utilizadores = FXCollections.observableArrayList();

    // A janela principal. Guarda a referência para poder trocar o ecrã.
    private Stage janela;

    // Os dados de quem entrou: o JSON devolvido pelo login (id, username, papel).
    // Fica null quando ninguém tem sessão iniciada.
    private Map<String, Object> eu;// eu guarda os dados do utilizador autenticado

    // A etiqueta de mensagens do ecrã principal.
    private Label avisoPrincipal;

    // ---------- Arranque e troca de Ecrãs----------

    // O JavaFX chama este método quando a aplicação arranca e entrega-lhe a janela.
    @Override
    public void start(Stage stage) {
        janela = stage;
        stage.setTitle("GESTÃO DE TAREFAS");//título do projeto
        trocar(ecraLogin());   // o primeiro ecrã é sempre o de login
        stage.show();          // mostra a janela
    }

    // Troca o ecrã que está dentro da janela.
    private void trocar(Scene ecra) {
        janela.setScene(ecra);       // põe o novo ecrã na janela
        janela.sizeToScene();        // ajusta o tamanho da janela ao novo ecrã
        janela.centerOnScreen();     // centra a janela no monitor
    }


    //  ECRÃ DE LOGIN
	
    private Scene ecraLogin() {
        // Título do formulário
        Label titulo = new Label("GESTÃO DE TAREFAS");
        titulo.setStyle("-fx-font-size:30px; -fx-font-weight:bold;");

        // Campo do nome de utilizador.
        TextField user = new TextField();
        user.setPromptText("NOME");

        // PasswordField mostra pontos em vez dos caracteres escritos.
        PasswordField pass = new PasswordField();
        pass.setPromptText("PALAVRA-PASSE");

        // Etiqueta (inicialmente invisível) para mostrar erros, como palavra-passe errada.
        Label aviso = novoAviso();

        Button entrar = new Button("ENTRAR");
        entrar.setMaxWidth(Double.MAX_VALUE);  // estica o botão até à largura toda da janela estática
        entrar.setDefaultButton(true);         // a tecla Enter também aciona este botão

        // Define o que acontece quando se carrega no botão.
        entrar.setOnAction(e -> {
            // Recolhe o que foi escrito. O Map.of cria um mapa imutável chave -> valor.
            Map<String, String> dados = Map.of("username", user.getText().trim(), "password", pass.getText());

            // Envia POST /login. O pedido corre em segundo plano; quando o servidor
            // responder, o código do segundo argumento corre no ecrã
            executar(() -> api.post("/login", dados), r -> {
                if (r.ok()) {
                    // Login certo: guardamos quem somos e mudamos de ecrã.
                    eu = (Map<String, Object>) r.json();
                    pass.clear();                 // apaga a palavra-passe do campo
                    trocar(ecraPrincipal());
                    carregarTudo();               // pede ao servidor as tarefas (e utilizadores)
                } else {
                    // Login errado: mostra o erro (já traduzido) a vermelho.
                   mostrar(aviso, r.erro(), true);
                }
            }, aviso);
        });

        // VBox = caixa vertical. O 12 é o espaço, em píxeis, entre componentes.
        VBox caixa = new VBox(15, titulo, user, pass, entrar, aviso);
        caixa.setPadding(new Insets(40));     // margem interior
        caixa.setAlignment(Pos.CENTER);       // tudo centrado
        caixa.setPrefWidth(400);              // largura preferida 400 px
        return new Scene(caixa);
    }

    //  ECRÃ PRINCIPAL

    private Scene ecraPrincipal() {
        // o servidor volta a verificar o papel em cada pedido, se estou em utilizador ou administrador.
        boolean admin = "ADMIN".equals(eu.get("papel"));
        avisoPrincipal = novoAviso();

        // ----- Barra do topo: saudação à esquerda, "Terminar sessão" à direita -----
        Label ola = new Label("SEJA BEM VINDO, " + eu.get("username") + (admin ? " (administrador)" : ""));
        ola.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:white;");

        // Um Region vazio com Priority.ALWAYS ocupa todo o espaço que sobra,
        // empurrando o botão seguinte para o canto direito.
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        Button sair = new Button("TERMINAR SESSÃO");
        //O logout faz um POST e, no callback, limpa o estado local: 
        sair.setOnAction(e -> executar(() -> api.post("/logout", Map.of()), r -> {
            eu = null;
			//Esta limpeza é importante: sem ela, o próximo utilizador veria os dados do anterior.
            tarefas.clear();
            utilizadores.clear();
            trocar(ecraLogin());
        }, avisoPrincipal));

        HBox topo = new HBox(12, ola, espaco, sair);
        topo.setAlignment(Pos.CENTER_LEFT);
        topo.setPadding(new Insets(12, 20, 12, 20));
        topo.setStyle("-fx-background-color:#2f4f6f;");

        // ----- Tabela de tarefas -----
        // Cada linha da tabela é um Map<String,Object> vindo do JSON do servidor.
        TableView<Map<String, Object>> tabela = new TableView<>(tarefas);
        tabela.setPlaceholder(new Label("AINDA NÃO EXISTE TAREFAS CRIADAS."));  // texto quando está vazia
        tabela.setPrefHeight(240);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.getColumns().add(coluna("TAREFA", t -> String.valueOf(t.get("titulo"))));
        if (admin) {
            // Só o administrador vê quem é o CRIADOR de cada tarefa.
            tabela.getColumns().add(coluna("CRIADOR", t -> String.valueOf(t.get("criador"))));
        }
        tabela.getColumns().add(coluna("ESTADO",
                t -> Boolean.TRUE.equals(t.get("concluida")) ? "FEITA" : "POR FAZER"));

        // ----- PUT: concluir ou reabrir -----
        // O PUT substitui a tarefa inteira, por isso enviaSSE o título atual
        // e inverte só o estado.
        Button alternar = new Button("CONCLUIR / REABRIR");
        alternar.setOnAction(e -> {
            Map<String, Object> t = tabela.getSelectionModel().getSelectedItem();
            if (t == null) {
                mostrar(avisoPrincipal, "SELECIONA PRIMEIRO UMA TAREFA DA TABELA.", true);
                return;
            }
            boolean feita = Boolean.TRUE.equals(t.get("concluida"));
            atualizarTarefa(t, String.valueOf(t.get("titulo")), !feita, null);
        });

        // ----- PUT: editar o título -----
        Button editar = new Button("EDITAR TÍTULO");
        editar.setOnAction(e -> {
            Map<String, Object> t = tabela.getSelectionModel().getSelectedItem();
            if (t == null) {
                mostrar(avisoPrincipal, "SELECIONA PRIMEIRO UMA TAREFA DA TABELA.", true);
                return;
            }
            // Caixa de diálogo com um campo de texto já preenchido com o título atual.
            TextInputDialog dialogo = new TextInputDialog(String.valueOf(t.get("titulo")));
            dialogo.setTitle("EDITAR TAREFA");
            dialogo.setHeaderText(null);
            dialogo.setContentText("NOVO TÍTULO:");
            dialogo.initOwner(janela);   // a caixa fica ligada à janela principal
            // showAndWait espera pela resposta. Se o utilizador cancelar, não acontece nada.
            dialogo.showAndWait()
                    .map(String::trim)
                    .filter(novo -> !novo.isEmpty())
                    .ifPresent(novo -> atualizarTarefa(t, novo,
                            Boolean.TRUE.equals(t.get("concluida")), "TAREFA ATUALIZADA."));
        });

        HBox acoes = new HBox(8, alternar, editar);

        // ----- DELETE: só o administrador vê este botão -----
        if (admin) {
            Button apagar = new Button("APAGAR");
            apagar.setOnAction(e -> {
                Map<String, Object> t = tabela.getSelectionModel().getSelectedItem();
                if (t == null) {
                mostrar(avisoPrincipal, "SELECIONA PRIMEIRO UMA TAREFA DA TABELA.", true);
                    return;
                }
                int id = ((Number) t.get("id")).intValue();
                // Pede confirmação; só se o utilizador disser Sim é que o DELETE é enviado.
                confirmar("APAGAR A TAREFA \"" + t.get("titulo") + "\"?", () ->
                        executar(() -> api.delete("/tarefas/" + id),
                                r -> depoisDeAcao(r, "TAREFA APAGADA."), avisoPrincipal));
            });
            acoes.getChildren().add(apagar);
        }

        // ----- POST: criar uma tarefa nova -----
        TextField novaTarefa = new TextField();
        novaTarefa.setPromptText("NOVA TAREFA");
        HBox.setHgrow(novaTarefa, Priority.ALWAYS);   // o campo ocupa o espaço que sobra

        // Um Runnable é um bocado de código guardado numa variável, para o usarmos
        // em dois sítios: no botão "Adicionar" e na tecla Enter do campo de texto.
        Runnable adicionar = () -> {
            String texto = novaTarefa.getText().trim();
            if (texto.isEmpty()) {
                mostrar(avisoPrincipal, "ESCREVE O TÍTULO DA TAREFA.", true);
                return;
            }
            executar(() -> api.post("/tarefas", Map.of("titulo", texto)), r -> {
                if (r.ok()) novaTarefa.clear();   // só limpa o campo se correu bem
                depoisDeAcao(r, "TAREFA CRIADA.");
            }, avisoPrincipal);
        };
        Button adicionarBtn = new Button("ADICIONAR");
        adicionarBtn.setOnAction(e -> adicionar.run());
        novaTarefa.setOnAction(e -> adicionar.run());   // Enter dentro do campo

        // Junta tudo num "cartão" branco.
        VBox cartaoTarefas = cartao(
                subtitulo(admin ? "TODAS AS TAREFAS" : "AS MINHAS TAREFAS"),
                tabela,
                acoes,
                new HBox(8, novaTarefa, adicionarBtn));

        VBox raiz = new VBox(16, avisoPrincipal, cartaoTarefas);
        raiz.setPadding(new Insets(16));
        raiz.setStyle("-fx-background-color:#f3f4f6;");

        // ----- Utilizadores (só o administrador): GET e POST -----
        // Não há botão de apagar nem de alterar utilizadores: o servidor nem sequer
        // tem esses métodos para este recurso (responderia 405).
        if (admin) {
            TableView<Map<String, Object>> tabelaU = new TableView<>(utilizadores);
            tabelaU.setPlaceholder(new Label("SEM UTILIZADORES."));
            tabelaU.setPrefHeight(150);
            tabelaU.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            tabelaU.getColumns().add(coluna("NOME", u -> String.valueOf(u.get("username"))));
            tabelaU.getColumns().add(coluna("TIPO",
                    u -> "ADMIN".equals(u.get("papel")) ? "ADMINISTRADOR" : "UTILIZADOR"));

            // Formulário para criar utilizadores.
            TextField nome = new TextField();
            nome.setPromptText("NOME DO UTILIZADOR");
            PasswordField senha = new PasswordField();
            senha.setPromptText("PALAVRA-PASSE (mín. 6 CARACTERES)");
            ComboBox<String> tipo = new ComboBox<>();
            tipo.getItems().addAll("UTILIZADOR", "ADMINISTRADOR");
            tipo.setValue("UTILIZADOR");   // valor escolhido por omissão
            HBox.setHgrow(nome, Priority.ALWAYS);
            HBox.setHgrow(senha, Priority.ALWAYS);

            Button criar = new Button("CRIAR");
            criar.setOnAction(e -> {
                // O texto mostrado no ecrã ("Administrador") é convertido para o
                // valor que a base de dados usa ("ADMIN").
                Map<String, String> dados = Map.of(
                        "username", nome.getText().trim(),
                        "password", senha.getText(),
                        "papel", "Administrador".equals(tipo.getValue()) ? "ADMIN" : "UTILIZADOR");
                // POST /utilizadores
                executar(() -> api.post("/utilizadores", dados), r -> {
                    if (r.ok()) {
                        nome.clear();
                        senha.clear();
                    }
                    depoisDeAcao(r, "UTILIZADOR CRIADO.");
                }, avisoPrincipal);
            });

            raiz.getChildren().add(cartao(
                    subtitulo("UTILIZADORES"),
                    tabelaU,
                    new HBox(8, nome, senha, tipo, criar)));
        }

        // O ecrã é a barra do topo seguida do conteúdo. Tamanho inicial 860 x (740 ou 540).
        return new Scene(new VBox(topo, raiz), 860, admin ? 740 : 540);
    }

    //  COMUNICAÇÃO COM O SERVIDOR

    // Pede ao servidor as tarefas e, se formos administrador, também os utilizadores.
    private void carregarTudo() {
        executar(() -> api.get("/tarefas"), r -> {
            if (r.ok()) {
                // setAll substitui o conteúdo da lista; a tabela atualiza-se sozinha.
                tarefas.setAll((List<Map<String, Object>>) r.json());
            } else {
                falhou(r);
            }
        }, avisoPrincipal);

        if (eu != null && "ADMIN".equals(eu.get("papel"))) {
            executar(() -> api.get("/utilizadores"), r -> {
                if (r.ok()) {
                    utilizadores.setAll((List<Map<String, Object>>) r.json());
                } else {
                    falhou(r);
                }
            }, avisoPrincipal);
        }
    }

    // PUT /tarefas/{id}: atualiza uma tarefa.
    // O PUT substitui o recurso por inteiro, por isso enviamos SEMPRE o título
    // e o estado, mesmo que só um deles tenha mudado.
    private void atualizarTarefa(Map<String, Object> tarefa, String titulo, boolean concluida, String mensagemOk) {
        int id = ((Number) tarefa.get("id")).intValue();
        Map<String, String> dados = Map.of("titulo", titulo, "concluida", String.valueOf(concluida));
        executar(() -> api.put("/tarefas/" + id, dados),
                r -> depoisDeAcao(r, mensagemOk), avisoPrincipal);
    }

    // Corre depois de uma operação que altera dados (criar, atualizar, apagar):
    // mostra a mensagem de sucesso e volta a pedir as listas ao servidor.
    private void depoisDeAcao(Api.Resposta r, String mensagemOk) {
        if (r.ok()) {
            if (mensagemOk != null) {
                mostrar(avisoPrincipal, mensagemOk, false);
            } else {
                avisoPrincipal.setVisible(false);
            }
            carregarTudo();
        } else {
            falhou(r);
        }
    }

    // Trata uma resposta de erro do servidor.
    private void falhou(Api.Resposta r) {
        if (r.status() == 401) {
            // 401 = sem sessão (expirou ou o Tomcat foi reiniciado): volta ao login.
            eu = null;
            trocar(ecraLogin());
        } else {
            mostrar(avisoPrincipal, r.erro(), true);
        }
    }

    // Executa um pedido HTTP em SEGUNDO PLANO.
    // Um pedido demora tempo. Se corresse no fio de execução do ecrã, a janela
    // ficava "congelada" à espera. Por isso:
    //   1. o pedido corre num fio (thread) à parte;
    //   2. quando há resposta, Platform.runLater volta ao fio do ecrã, o único
    //      que pode mexer nos componentes gráficos;
    //   3. se o servidor não responder, mostra uma mensagem de erro.
    private void executar(Pedido pedido, Consumer<Api.Resposta> depois, Label aviso) {
        Thread fio = new Thread(() -> {
            try {
                Api.Resposta r = pedido.chamar();
                Platform.runLater(() -> depois.accept(r));
            } catch (Exception e) {
                Platform.runLater(() -> mostrar(aviso,
                        "NÃO EXISTE COMUNICAÇÃO COM O SERVIDOR!", true));
            }
        });
        fio.setDaemon(true);   // o fio não impede a aplicação de fechar
        fio.start();
    }

    //  PEQUENOS AUXILIARES
	
    // Cria uma coluna de tabela. O segundo argumento diz como tirar o texto
    // a mostrar a partir de uma linha (um Map).
    private TableColumn<Map<String, Object>, String> coluna(String nome,
            Function<Map<String, Object>, String> valor) {
        TableColumn<Map<String, Object>, String> c = new TableColumn<>(nome);
        c.setCellValueFactory(x -> new SimpleStringProperty(valor.apply(x.getValue())));
        return c;
    }

    // Um "cartão": caixa vertical branca com cantos arredondados.
    private VBox cartao(Node... filhos) {
        VBox c = new VBox(10, filhos);
        c.setPadding(new Insets(16));
        c.setStyle("-fx-background-color:white; -fx-background-radius:8;");
        return c;
    }

    // Título de uma secção.
    private Label subtitulo(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");
        return l;
    }

    // Etiqueta de avisos, inicialmente invisível. O managedProperty ligado ao
    // visibleProperty faz com que, enquanto invisível, ela não ocupe espaço.
    private Label novoAviso() {
        Label l = new Label();
        l.setWrapText(true);                     // quebra o texto em várias linhas se for longo
        l.setMaxWidth(Double.MAX_VALUE);
        l.setVisible(false);
        l.managedProperty().bind(l.visibleProperty());
        return l;
    }

    // Mostra uma mensagem numa etiqueta de avisos: vermelha se for erro, verde se for sucesso.
    private void mostrar(Label l, String texto, boolean erro) {
        l.setText(texto);
        l.setStyle(erro
                ? "-fx-background-color:#f8d7da; -fx-text-fill:#842029; -fx-padding:8 12; -fx-background-radius:5;"
                : "-fx-background-color:#dff0d8; -fx-text-fill:#2d5a27; -fx-padding:8 12; -fx-background-radius:5;");
        l.setVisible(true);
    }

    // Caixa de diálogo Sim/Não. Só executa a ação se o utilizador responder Sim.
    private void confirmar(String texto, Runnable aoConfirmar) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.initOwner(janela);
        a.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> aoConfirmar.run());
    }


    // Ponto de entrada do programa: o launch() arranca o JavaFX, que depois chama start().
    public static void main(String[] args) {
        launch(args);
    }
}