# LembreBem

Aplicativo Android nativo gratuito para apoio à memória, com uso local sem conta e integração opcional para familiares. **Entrega de desenvolvimento, ainda sem compilação Android verificada ou APK gerado neste ambiente.** Não é dispositivo médico nem serviço garantido de emergência.

O código anterior foi preservado em Git e em backup externo ao repositório. Veja `IMPLEMENTATION_REPORT.md` para resultados e limites, sem confundir recursos escritos com funções validadas.

## Gerar o APK no GitHub Actions

1. Crie um repositório GitHub e envie **o conteúdo desta pasta na raiz**, incluindo `.github/`. Não envie o ZIP como único arquivo. Repositório público usa runners padrão gratuitos; repositório privado depende da quota gratuita da conta. Não habilite cobrança nem runners pagos.
2. Abra **Actions → LembreBem tests and Debug APK → Run workflow**, na branch `main`. Push e pull request também executam o workflow.
3. O runner Linux configura Java Temurin 17, Android SDK 35/build-tools 35.0.0 e Gradle 8.11.1. O projeto usa AGP 8.9.2, Kotlin 2.1.20 e KSP 2.1.20-1.0.32.
4. Aguarde todos os jobs. Baixe `lembrebem-debug-apk` em **Artifacts** somente após sucesso. Extraia `app-debug.apk`, transfira para Android 8+ e permita instalação por essa origem. É APK Debug assinado pelo runner, exclusivo para testes fictícios. Instalações de execuções diferentes podem exigir desinstalar a anterior por mudança de chave; exporte dados antes.
5. O artefato de relatórios é mantido por sete dias, inclusive quando o build falha. Logs do Actions mostram problemas reais de compilação/lint. O workflow compila os testes instrumentados, mas **não os executa em emulador**.

Nenhuma credencial Firebase é necessária para compilar ou usar o modo local. O Gradle é instalado pelo workflow; não há Gradle Wrapper incluído. Para compilar localmente com SDK e Gradle instalados: `cd android && gradle :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug :app:assembleDebug`.

## Testes disponíveis sem Android SDK

Requer Node 24.22.1+ e Python 3:

```sh
./scripts/check.sh
cd backend
npm run demo
```

API demo escuta apenas loopback e usa dados fictícios. Push demo não envia notificações. O APK exige HTTPS e não se conecta diretamente a esse servidor HTTP de demonstração. Veja `backend/README.md` para contratos, Firebase Auth, FCM HTTP v1, Workers/D1 e configuração manual. Nada foi implantado em conta externa.

## Teste manual mínimo

Escolha idioma e modo Familiar, cadastre medicamento fictício com fonte humana conferida e contato identificado. Autorize notificações/alarmes pelo fluxo Android; mude para Pessoa assistida. Verifique alarme e confirmação offline, voz ausente, permissões negadas, reinício e tela bloqueada. Grave apenas áudio fictício consentido, importe texto e revise rascunho sem ativação automática. Valide dois aparelhos antes de confiar em avisos remotos. Veja `TEST_PLAN.md` e `android/README.md`.

## Idiomas

Recursos iniciais: pt-BR padrão, pt-PT, en-US, ja-JP e es-ES. Nomes de medicamentos, doses e textos clínicos permanecem literais. Traduções de saúde/segurança precisam revisão humana; paridade de chaves não comprova qualidade linguística ou layout. País é separado do idioma.

Para acrescentar en-GB, es-419 ou outro idioma: crie o diretório Android apropriado `values-b+en+GB`/`values-b+es+419`, traduza todas as chaves, atualize locales_config, seletor e script de paridade; adicione modelos e lista de idiomas aceitos no backend e respectivos testes. Não altere regras de horário/doses por idioma. Revise TTS, tipografia, formatos e legislação local.

Documentação: `ARCHITECTURE.md`, `SECURITY_PRIVACY.md`, `docs/IMPROVEMENTS.md`, `docs/clinical/README.md`, relatórios independentes em `docs/reviews/`. Referências oficiais estão no comando mestre; versões/quotas recentes não foram confirmadas online devido ao bloqueio do ambiente.
