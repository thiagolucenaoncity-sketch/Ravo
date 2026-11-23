package com.moto.painel

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class SettingsActivity : Activity() {

    private val reqPortrait = 42
    private val reqLandscape = 43

    // Página aberta: null = menu principal
    private var section: String? = null

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private val accent: Int get() = Prefs.accentColor(this)
    private val ravoUpdater by lazy { RavoUpdater(this) }

    override fun onResume() {
        super.onResume()
        // Nas configurações, consulta à internet apenas após tocar no botão.
        ravoUpdater.onResume(checkAutomatically = false)
    }

    override fun onPause() {
        ravoUpdater.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        ravoUpdater.close()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    // Botão voltar do celular: volta ao menu antes de sair
    override fun onBackPressed() {
        if (section != null) {
            go(null)
        } else {
            super.onBackPressed()
        }
    }

    private fun go(s: String?) {
        section = s
        setContentView(buildUi())
    }

    private fun buildUi(): ScrollView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF06142B.toInt())
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        col.addView(TextView(this).apply {
            text = titleOf(section)
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })

        if (section == null) {
            add(col, menuBtn("Aparência", R.drawable.ravo_menu_palette) { go("aparencia") }, 20)
            add(col, menuBtn("Velocímetro", R.drawable.ravo_menu_speed) { go("velocimetro") }, 10)
            add(col, menuBtn("Mapa", R.drawable.ravo_menu_map) { go("mapa") }, 10)
            // Laboratório independente, sem modificar o motor do mapa principal.
            add(col, plainBtn("RAVO Maps Lab · Open / Core (experimental)") {
                startActivity(Intent(this, com.moto.painel.maps.RavoMapsLabActivity::class.java))
            }, 10)
            add(col, menuBtn("Abertura do app", R.drawable.ravo_menu_play) { go("abertura") }, 10)
            add(col, menuBtn("Unidades", R.drawable.ravo_menu_ruler) { go("unidades") }, 10)
            add(col, menuBtn("Veículo", R.drawable.ravo_menu_bike) { go("veiculo") }, 10)
            add(col, plainBtn(
                if (Diagnostics.enabled(this)) "Diagnóstico: LIGADO (toque para desligar)"
                else "Diagnóstico: DESLIGADO (toque para ativar)"
            ) {
                Diagnostics.setEnabled(this, !Diagnostics.enabled(this))
                go(null)
            }, 14)
            add(col, plainBtn("Testar RAVO completo (2 minutos)") {
                Diagnostics.setEnabled(this, true)
                val launch = Intent(this, MainActivity::class.java).apply {
                    putExtra("ravo_full_app_stress", true)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(launch)
                finish()
            }, 8)
            add(col, plainBtn("Compartilhar relatório de diagnóstico") {
                val report = Diagnostics.exportFile(this).readText().takeLast(120000)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "RAVO - relatório de diagnóstico")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                startActivity(Intent.createChooser(send, "Compartilhar diagnóstico do RAVO"))
            }, 8)
            add(col, plainBtn("Apagar dados de diagnóstico") {
                Diagnostics.clear(this)
                Toast.makeText(this, "Relatórios locais apagados", Toast.LENGTH_SHORT).show()
            }, 8)
            add(col, plainBtn("Verificar atualização do RAVO") {
                ravoUpdater.checkNow(force = true)
            }, 18)
            add(col, plainBtn("Voltar ao painel") { finish() }, 24)
        } else {
            when (section) {
                "aparencia" -> addAppearance(col)
                "velocimetro" -> addVelocimetro(col)
                "mapa" -> addMapa(col)
                "abertura" -> addAbertura(col)
                "unidades" -> addUnidades(col)
                "veiculo" -> addVeiculo(col)
            }
            add(col, plainBtn("← Voltar às configurações") { go(null) }, 24)
        }

        return ScrollView(this).apply {
            addView(col, ViewGroup.LayoutParams(match, wrap))
        }
    }

    private fun titleOf(s: String?): String = when (s) {
        "aparencia" -> "Aparência"
        "velocimetro" -> "Velocímetro"
        "mapa" -> "Mapa"
        "abertura" -> "Abertura do app"
        "unidades" -> "Unidades"
        "veiculo" -> "Veículo"
        else -> "Configurações"
    }

    // ---------- Páginas ----------

    private fun addAppearance(col: LinearLayout) {
        add(col, section("PLANO DE FUNDO"), 0)
        add(col, cycler("Plano de fundo", Prefs.BG_NAMES, "bg"))
        add(col, cycler("Cor do fundo", Prefs.COLOR_NAMES, "bg_c"))
        add(col, cycler("Cor da grade", Prefs.COLOR_NAMES, "grid_c"))

        add(col, section("CORES"), 0)
        add(col, cycler("Cor de destaque", Prefs.COLOR_NAMES, "accent_c"))
        add(col, cycler("Cor dos cartões", Prefs.COLOR_NAMES, "card_c"))
        add(col, cycler("Cor do relógio", Prefs.COLOR_NAMES, "clock_c"))
        add(col, cycler("Cor dos textos", Prefs.COLOR_NAMES, "label_c"))

        add(col, plainBtn("Restaurar cores padrão") {
            Prefs.resetColors(this)
            Toast.makeText(this, "Cores restauradas", Toast.LENGTH_SHORT).show()
            go("aparencia")
        }, 20)
    }

    private fun addVelocimetro(col: LinearLayout) {
        add(col, section("FORMATO"), 0)
        add(col, cycler("Estilo", Prefs.GAUGE_NAMES, "gauge"))
        add(col, numCycler("Espessura do indicador", Prefs.GAUGE_THICKNESS, "gauge_stroke", 12, " dp"))
        add(col, numCycler("Escala máxima", Prefs.GAUGE_MAX_SPEED, "gauge_max", 200, " km/h"))
        add(col, numCycler("Mapa atrás do velocímetro", Prefs.MAP_OPACITY, "gauge_map_opacity", 35, "%"))
        add(col, note("Escolha Arco, Digital, Anel 360°, Semicírculo, Barra esportiva ou HUD digital. Personalize as cores, os limites, a espessura e a transparência do mapa. O valor mostrado continua sendo a velocidade medida pelo GPS."), 12)

        add(col, section("CORES POR VELOCIDADE"), 0)
        add(col, numCycler("Faixa 1 vai até", Prefs.LIMIT1_VALUES, "arc_lim1", 80, " km/h"))
        add(col, cycler("Cor da faixa 1", Prefs.COLOR_NAMES, "arc_c1"))
        add(col, numCycler("Faixa 2 vai até", Prefs.LIMIT2_VALUES, "arc_lim2", 120, " km/h"))
        add(col, cycler("Cor da faixa 2", Prefs.COLOR_NAMES, "arc_c2"))
        add(col, cycler("Cor da faixa 3 (acima da faixa 2)", Prefs.COLOR_NAMES, "arc_c3"))
        add(col, cycler("Cor do arco vazio (parte não preenchida)", Prefs.COLOR_NAMES, "arc_track"))
        add(col, note("Faixa 1: de 0 até o limite 1. Faixa 2: do limite 1 até o limite 2. Faixa 3: acima do limite 2. O arco inteiro muda de cor conforme a faixa em que você está."), 12)

        add(col, section("NÚMEROS E TEXTOS"), 0)
        add(col, cycler("Cor dos números", Prefs.COLOR_NAMES, "num_color"))
        add(col, cycler("Cor do km/h", Prefs.COLOR_NAMES, "unit_color"))
        add(col, cycler("Cor das estatísticas", Prefs.COLOR_NAMES, "stat_c"))
    }

    private fun addMapa(col: LinearLayout) {
        add(col, section("3D"), 0)
        add(col, cycler("Prédios 3D", arrayOf("Desligados", "Ligados"), "buildings"))
        add(col, note("Os prédios só aparecem quando o modo 3D estiver ativado no mapa."), 12)
        add(col, section("ALERTAS DE TRÂNSITO"), 16)
        add(col, cycler("Radares fixos", arrayOf("Ligados", "Desligados"), "alert_radar"))
        add(col, cycler("Lombadas", arrayOf("Ligadas", "Desligadas"), "alert_bump"))
        add(col, cycler("Semáforos (ícone no mapa)", arrayOf("Ligados", "Desligados"), "alert_semaforo"))
        add(col, cycler("Bipes dos radares na rota", arrayOf("Ligados", "Desligados"), "alert_beep"))
        add(col, note("Sons de alertas (voz e bipes): somente radares à frente na rota em andamento. Sem rota ativa, nenhum alerta sonoro de trânsito. Lombadas e semáforos continuam somente no mapa. A voz das instruções de navegação permanece independente."), 12)
        val updatedAt = RadarDatabaseUpdater.lastUpdateMs(this)
        val updatedLabel = if (updatedAt > 0L)
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR")).format(java.util.Date(updatedAt))
        else "base incluída no APK"
        add(col, note("Radares e lombadas de São Paulo são consultados diretamente no celular, mesmo sem internet. A base Pardal (ODbL) é verificada para atualização automática a cada 15 dias quando o app estiver aberto e conectado. Última atualização: $updatedLabel. Em caso de falha, os dados anteriores permanecem."), 12)
        add(col, note("Limite de velocidade: a plaquinha ao lado de cada radar mostra a velocidade cadastrada para aquele radar (quando disponível). A placa VIA mostra o maxspeed da via mapeada no OpenStreetMap, de forma separada. Sem dado confiável, a placa VIA fica oculta. O último trecho consultado é mantido em cache para uso sem conexão; a sinalização da rua prevalece."), 12)
        add(col, note("Semáforos: o RAVO prioriza a base de São Paulo incluída no APK e guarda as atualizações no celular (até a cada 15 dias, com conexão). Se a base estadual ainda não estiver disponível, mantém a consulta por região e o cache. Os ícones NÃO indicam se o sinal está verde, amarelo ou vermelho; pontos podem faltar ou estar desatualizados."), 12)
    }

    private fun addAbertura(col: LinearLayout) {
        add(col, section("TELA DE ABERTURA"), 0)
        add(col, cycler("Tela de abertura", arrayOf("Abertura RAVO", "RAVO + Minha Moto", "Desligada"), "splash_mode"))
        add(col, pickButton("Foto para celular em pé", reqPortrait), 10)
        add(col, pickButton("Foto para celular deitado", reqLandscape), 10)
        add(col, note("Na opção RAVO + Minha Moto, a abertura oficial aparece primeiro e depois sua foto. Escolha uma foto para cada posição do celular."), 12)
    }

    private fun addUnidades(col: LinearLayout) {
        add(col, section("MEDIDAS"), 0)
        add(col, cycler("Medidas", Prefs.UNIT_NAMES, "units"))
    }

    private fun addVeiculo(col: LinearLayout) {
        add(col, section("SEU VEÍCULO"), 0)
        val nickInput = field("Apelido (ex: JUREMA)", Prefs.getStr(this, "nick"))
        val brandInput = field("Marca (ex: Yamaha)", Prefs.getStr(this, "brand"))
        val modelInput = field("Modelo (ex: FZ25)", Prefs.getStr(this, "model"))
        add(col, nickInput, 8)
        add(col, brandInput, 8)
        add(col, modelInput, 8)
        add(col, plainBtn("Salvar veículo") {
            Prefs.save(this, "nick", nickInput.text.toString().trim())
            Prefs.save(this, "brand", brandInput.text.toString().trim())
            Prefs.save(this, "model", modelInput.text.toString().trim())
            Toast.makeText(this, "Veículo salvo", Toast.LENGTH_SHORT).show()
        }, 10)
        add(col, section("TEMA VEÍCULO"), 16)
        add(col, note("Personalize o marcador mostrado no mapa."), 8)
        add(col, plainBtn("Ícone no mapa: ${Prefs.MARKER_NAMES[Prefs.getInt(this, "marker").coerceIn(0, Prefs.MARKER_NAMES.lastIndex)]}") {
            val next = (Prefs.getInt(this, "marker") + 1) % Prefs.MARKER_NAMES.size
            Prefs.save(this, "marker", next)
            go("veiculo")
        }, 10)
        add(col, cycler("Cor da moto/veículo", Prefs.COLOR_NAMES, "marker_c"))
        if (Prefs.getInt(this, "marker") == 10) {
            add(col, cycler("Cor da bag", Prefs.COLOR_NAMES, "bag_c"))
            add(col, note("Cor da moto e da bag são independentes."), 8)
        }
    }

    // ---------- Fotos da abertura ----------

    private fun pickButton(label: String, requestCode: Int): Button = plainBtn(label) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, requestCode)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == reqPortrait || requestCode == reqLandscape) && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val target = if (requestCode == reqPortrait) "splash_p.jpg" else "splash_l.jpg"
            if (saveSplashPhoto(uri, target)) {
                Prefs.save(this, "splash_mode", 1)
                Toast.makeText(this, "Foto salva", Toast.LENGTH_SHORT).show()
                setContentView(buildUi())
            } else {
                Toast.makeText(this, "Não consegui abrir essa foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Copia a foto para dentro do app, reduzindo o tamanho para não pesar
    private fun saveSplashPhoto(uri: Uri, fileName: String): Boolean {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false
            File(filesDir, fileName).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bmp.recycle()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    // ---------- Componentes ----------

    private fun add(col: LinearLayout, v: View, topDp: Int = 10) {
        col.addView(v, LinearLayout.LayoutParams(match, wrap).apply { topMargin = dp(topDp) })
    }

    private fun section(title: String) = TextView(this).apply {
        text = title
        textSize = 13f
        setTextColor(accent)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(24), 0, 0)
    }

    private fun note(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(0xFF7F93B5.toInt())
    }

    // Mesmo acabamento arredondado em todas as páginas de configurações.
    private fun styleOptionButton(button: Button) {
        button.isAllCaps = false
        button.setTextColor(Color.WHITE)
        button.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
        button.setPadding(dp(18), dp(12), dp(18), dp(12))
        button.minHeight = dp(56)
        button.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFF14284A.toInt())
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), (accent and 0x00FFFFFF) or 0x55000000)
        }
    }

    // Botão que abre uma página de configurações
    private fun menuBtn(label: String, iconRes: Int, onClick: () -> Unit) = Button(this).apply {
        val icon = getDrawable(iconRes)?.mutate()
        icon?.setTint(accent)
        icon?.setBounds(0, 0, dp(24), dp(24))
        setCompoundDrawables(icon, null, null, null)
        compoundDrawablePadding = dp(16)
        gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
        setPadding(dp(18), dp(12), dp(18), dp(12))
        minHeight = dp(60)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFF14284A.toInt())
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), (accent and 0x00FFFFFF) or 0x55000000)
        }
        text = label
        isAllCaps = false
        textSize = 18f
        setTextColor(Color.WHITE)
        setOnClickListener { onClick() }
    }

    private fun plainBtn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        styleOptionButton(this)
        setOnClickListener { onClick() }
    }

    // Botão que vai trocando de opção a cada toque
    private fun cycler(label: String, names: Array<String>, key: String): Button {
        val b = Button(this)
        fun show(i: Int) {
            b.text = "$label: ${names[i]}"
        }
        show(Prefs.getInt(this, key).coerceIn(0, names.size - 1))
        styleOptionButton(b)
        b.setOnClickListener {
            val next = (Prefs.getInt(this, key) + 1) % names.size
            Prefs.save(this, key, next)
            show(next)
        }
        return b
    }

    // Botão que percorre uma lista de valores numéricos (ex.: limites de km/h)
    private fun numCycler(label: String, values: IntArray, key: String, def: Int, suffix: String): Button {
        val b = Button(this)
        fun cur(): Int = Prefs.intOr(this, key, def)
        fun show() {
            b.text = "$label: ${cur()}$suffix"
        }
        show()
        styleOptionButton(b)
        b.setOnClickListener {
            val idx = values.indexOf(cur())
            val next = values[(if (idx < 0) 0 else idx + 1) % values.size]
            Prefs.save(this, key, next)
            show()
        }
        return b
    }

    private fun field(hint: String, value: String) = EditText(this).apply {
        setHint(hint)
        setText(value)
        setTextColor(Color.WHITE)
        setHintTextColor(0xFF7F93B5.toInt())
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFF14284A.toInt())
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), (accent and 0x00FFFFFF) or 0x55000000)
        }
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setSingleLine(true)
    }
}
