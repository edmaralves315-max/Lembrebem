# Voz e documentos

Módulo `org.lembrebem.clinical`: Photo Picker sem permissão ampla de fotos; ML Kit bundled Latin/Japanese escolhido pelo idioma do aplicativo; imagem original e textos protegidos AES-256-GCM com chave não exportável no Android Keystore; gravação AAC explicitamente consentida e visível, interrompida ao abandonar a tela. A negação de microfone preserva edição/importação manual. A concessão de permissão exige novo toque para iniciar.

TranscriptionEngine substituível: implementação entregue ManualTranscriptionEngine declara zero idiomas automáticos; nenhum texto fictício é retornado. Consulta usa importação UTF-8 ou digitação real. SummaryEngine apenas agrupa linhas literais com referência de linha; não calcula confiança, não interpreta doses/datas e não possui acesso a MedicationPlan. Rascunho e revisão do familiar ficam locais. Para ativar um lembrete, familiar confere original e cadastra na tela específica.

Áudios cifrados podem ser reproduzidos localmente. Temporários privados de gravação/reprodução são apagados ao finalizar ou reabrir módulo após interrupção. Durante captura/reprodução o MediaRecorder/MediaPlayer usa temporário privado sem cifra: limitação explícita de arquivos em uso. Retenção 7/30/90 dias é aplicada ao abrir a tela; exclusão total requer confirmação. Dados clínicos ficam em noBackupFilesDir. Texto de trabalho só persiste por botão Salvar; não há upload automático. OCR é sugestão não validada e não promove medicação.

Testes de domínio disponíveis em ClinicalDomainTest.kt para copiar ao sourceSet test; testes de microfone, OCR físico, Keystore, rotação e reprodução requerem Android. Traduções iniciais de todas as strings em cinco variantes exigem revisão humana antes de uso público.

Atualização da consolidação: ClinicalDomainTest.kt já está no sourceSet `android/app/src/test/java/org/lembrebem/clinical/`; não precisa copiá-lo novamente. Ainda não foi executado sem Gradle/SDK. Retenção foi reorganizada em coluna com botões amplos22sp; evidência estrutural no relatório de usabilidade B, sem teste visual físico.
