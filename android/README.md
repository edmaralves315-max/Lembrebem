# Android LembreBem

Aplicativo nativo Kotlin/Compose (SDK mínimo 26; compile/target 35). As telas e alertas têm recursos pt-BR (padrão), pt-PT, en-US, ja-JP, es-ES. Traduções iniciais precisam revisão humana antes de distribuição pública.

## Compilar e testar

Com Android SDK platform 35, build-tools 35.0.0, Java 17+ e Gradle 8.11.1:

```sh
cd android
gradle :app:testDebugUnitTest :app:assembleDebug
gradle :app:connectedDebugAndroidTest
```

O APK fica em `app/build/outputs/apk/debug/app-debug.apk`. Não há credenciais de Firebase no projeto. Não é necessário Google Services plugin no modo local. Na tela Configurações → Avisos familiares, informe URL HTTPS do Worker e configuração **pública** Firebase (API key, application ID, project ID), entre com e-mail/senha ou crie conta. O servidor valida o token Firebase e autorização por pessoa. Nunca informe chave privada de conta de serviço no APK.

## Operação local

1. Escolha idioma e modo Familiar.
2. Cadastre medicamento fictício com nome/dose literais, horário HH:mm, dias 1–7, datas ISO e fuso IANA; revise a fonte e marque conferência humana.
3. Autorize notificações e alarmes exatos na Central de permissões. Sem alarmes exatos concedidos usa-se alarme inexato com aviso de estado da permissão.
4. Troque para Pessoa assistida. Alarmes e confirmações persistem sem conta/internet. "Já tomei" registra instante real; correção gera nova auditoria. Adiar mantém identidade da ocorrência para reconciliação com o servidor.
5. Cadastre contatos com identificação textual e foto opcional, telefone fornecido dentro do app. Chamadas e SMS abrem aplicativos oficiais para ação do usuário.

## Vínculos remotos

Autorize sincronização mínima na Privacidade. Depois de sincronizar o perfil proprietário, crie convite consentido de uso único em Vínculos e avisos familiares. No segundo aparelho, entre com outra conta, aceite o convite e selecione a pessoa vinculada. O aparelho ligado como cuidador não envia sua bateria/localização ao perfil assistido e não agenda alarmes medicamentosos do assistido. Avisos são enviados por FCM do servidor. A lista autenticada mostra avisos e carimbos de tempo, mesmo se FCM ainda não configurado.

Um aparelho mantém **um perfil local por vez**. A troca para outra pessoa com planos existentes é recusada para evitar misturar informações de saúde. Exporte e exclua dados locais antes de mudar; gestão de múltiplos assistidos em um único aparelho não é prometida nesta versão.

WorkManager executa sincronização oportunista (mínimo 15 minutos) com outbox idempotente; "Sincronizar agora" agenda trabalho imediato, ainda sujeito ao Android. Cron do servidor é responsável pelos avisos familiares no horário e após 15 minutos, independentemente da conexão do assistido. Notificações públicas ocultam conteúdo clínico; mensagens FCM genéricas apontam ao app.

## Voz, localização e segurança

A voz usa apenas vozes offline instaladas no idioma; se faltarem, há aviso visual e bipe. Notificações permanecem independentes de TTS. Geofence exige consentimento explícito, localização precisa e, separadamente, segundo plano; mínimo 100 m para reduzir instabilidade, detecção pode atrasar. É reprogramado após reinício, mudança de relógio/fuso e abertura. A família abre rota para ir até a pessoa; o app nunca orienta a pessoa desorientada a voltar sozinha. Referência textual de terceiro é marcada como não verificável, sem inferir geocodificação inexistente.

O nome/dose de medicamentos nunca é traduzido. Interface usa idioma do app; país é configuração separada. Datas exibidas usam formato regional e preferência 12/24 h; regras de recorrência usam ZoneId e não idioma.

Consulte `../docs/clinical/README.md` para gravação/OCR e restrições clínicas. Modo cuidador local é uma escolha de interface, não autenticação; o controle de acesso remoto é realizado no backend. Proteja o aparelho com bloqueio Android.

## Validação física necessária

AlarmManager com tela bloqueada, otimização agressiva de bateria, boot, GPS/geofence, notificações Android 13–16, TalkBack/fonte grande, microfone/Keystore/OCR e dois aparelhos devem ser testados em hardware/emulador. Não são inferidos como validados por testes JVM. O app não é dispositivo médico nem serviço garantido de emergência.

## CI sem SDK local

Workflow disponível em `../.github/workflows/android.yml`; instruções completas no README raiz. Configura Gradle8.11.1 automaticamente, executa testes JVM/lint, compila instrumentados e gera Debug APK somente se as tarefas passarem. Nenhuma execução CI ou APK é alegada nesta entrega. Sincronização final pagina eventos com cursor opaco após aplicar cada página; ver contrato API atualizado.
