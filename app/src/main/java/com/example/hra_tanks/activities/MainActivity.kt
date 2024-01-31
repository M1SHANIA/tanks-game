package ru.appngo.tankstutorial.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import android.view.KeyEvent.KEYCODE_SPACE
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hra_tanks.R
import ru.appngo.tankstutorial.GameCore
import ru.appngo.tankstutorial.LevelStorage
import ru.appngo.tankstutorial.ProgressIndicator
import ru.appngo.tankstutorial.drawers.BulletDrawer
import ru.appngo.tankstutorial.drawers.ElementsDrawer
import ru.appngo.tankstutorial.drawers.EnemyDrawer
import ru.appngo.tankstutorial.drawers.GridDrawer
import ru.appngo.tankstutorial.enums.Direction
import ru.appngo.tankstutorial.enums.Direction.BOTTOM
import ru.appngo.tankstutorial.enums.Direction.LEFT
import ru.appngo.tankstutorial.enums.Direction.RIGHT
import ru.appngo.tankstutorial.enums.Direction.UP
import ru.appngo.tankstutorial.enums.Material.BRICK
import ru.appngo.tankstutorial.enums.Material.CONCRETE
import ru.appngo.tankstutorial.enums.Material.EAGLE
import ru.appngo.tankstutorial.enums.Material.EMPTY
import ru.appngo.tankstutorial.enums.Material.GRASS
import ru.appngo.tankstutorial.enums.Material.PLAYER_TANK
import ru.appngo.tankstutorial.models.Coordinate
import ru.appngo.tankstutorial.models.Element
import ru.appngo.tankstutorial.models.Tank
import ru.appngo.tankstutorial.sounds.MainSoundPlayer

const val CELL_SIZE = 50
const val VERTICAL_CELL_AMOUNT = 38
const val HORIZONTAL_CELL_AMOUNT = 25
const val VERTICAL_MAX_SIZE = CELL_SIZE * VERTICAL_CELL_AMOUNT
const val HORIZONTAL_MAX_SIZE = CELL_SIZE * HORIZONTAL_CELL_AMOUNT
const val HALF_WIDTH_OF_CONTAINER = VERTICAL_MAX_SIZE / 2

class MainActivity : AppCompatActivity(), ProgressIndicator {
    private var editMode = false
    private lateinit var item: MenuItem
    private lateinit var container: FrameLayout
    private lateinit var editorClear: ImageView
    private lateinit var editorBrick: ImageView
    private lateinit var editorConcrete: ImageView
    private lateinit var editorGrass: ImageView
    private lateinit var initTitle: TextView
    private lateinit var materialsContainer: LinearLayout
    private lateinit var totalContainer: FrameLayout
    private var gameStarted = false
    private val playerTank by lazy {
        Tank(
                Element(
                        material = PLAYER_TANK,
                        coordinate = getPlayerTankCoordinate()
                ), UP, enemyDrawer
        )
    }

    private val bulletDrawer by lazy {
        BulletDrawer(
                container,
                elementsDrawer.elementsOnContainer,
                enemyDrawer,
                soundManager,
                gameCore
        )
    }

    private val gameCore by lazy {
        GameCore(this)
    }

    private val soundManager by lazy {
        MainSoundPlayer(this, this)
    }

    private fun getPlayerTankCoordinate() = Coordinate(
            top = HORIZONTAL_MAX_SIZE - PLAYER_TANK.height * CELL_SIZE,
            left = HALF_WIDTH_OF_CONTAINER - 8 * CELL_SIZE
    )

    private val eagle = Element(
            material = EAGLE,
            coordinate = getEagleCoordinate()
    )


    private fun getEagleCoordinate() = Coordinate(
            top = HORIZONTAL_MAX_SIZE - EAGLE.height * CELL_SIZE,
            left = HALF_WIDTH_OF_CONTAINER - EAGLE.width * CELL_SIZE / 2
    )

    private val gridDrawer by lazy {
        GridDrawer(container)
    }

    private val elementsDrawer by lazy {
        ElementsDrawer(container)
    }

    private val levelStorage by lazy {
        LevelStorage(this)
    }

    private val enemyDrawer by lazy {
        EnemyDrawer(
                container,
                elementsDrawer.elementsOnContainer,
                soundManager,
                gameCore
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.container)
        editorClear = findViewById(R.id.editor_clear)
        editorBrick = findViewById(R.id.editor_brick)
        editorConcrete = findViewById(R.id.editor_concrete)
        editorGrass = findViewById(R.id.editor_grass)
        initTitle = findViewById(R.id.init_title)
        materialsContainer = findViewById(R.id.materials_container)

        enemyDrawer.bulletDrawer = bulletDrawer
        container.layoutParams = FrameLayout.LayoutParams(
            VERTICAL_MAX_SIZE,
            HORIZONTAL_MAX_SIZE
        )
        editorClear.setOnClickListener { elementsDrawer.currentMaterial = EMPTY }
        editorBrick.setOnClickListener { elementsDrawer.currentMaterial = BRICK }
        editorConcrete.setOnClickListener { elementsDrawer.currentMaterial = CONCRETE }
        editorGrass.setOnClickListener { elementsDrawer.currentMaterial = GRASS }
        container.setOnTouchListener { _, event ->
            if (!editMode) {
                return@setOnTouchListener true
            }
            elementsDrawer.onTouchContainer(event.x, event.y)
            return@setOnTouchListener true
        }
        elementsDrawer.drawElementsList(levelStorage.loadLevel())
        elementsDrawer.drawElementsList(listOf(playerTank.element, eagle))
        hideSettings()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings, menu)
        item = menu.findItem(R.id.menu_play)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                switchEditMode()
                true
            }
            R.id.menu_save -> {
                levelStorage.saveLevel(elementsDrawer.elementsOnContainer)
                true
            }
            R.id.menu_play -> {
                if (editMode) {
                    return true
                }
                showIntro()
                if(soundManager.areSoundsReady()) {
                    gameCore.startOrPauseTheGame()
                    if (gameCore.isPlaying()) {
                        resumeTheGame()
                    } else {
                        pauseTheGame()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun resumeTheGame() {
        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_pause)
        gameCore.resumeTheGame()
    }

    private fun showIntro() {
        if(gameStarted){
            return
        }
        gameStarted = true
        soundManager.loadSounds()
    }

    private fun pauseTheGame() {
        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_play)
        gameCore.pauseTheGame()
        soundManager.pauseSounds()
    }

    override fun onPause() {
        super.onPause()
        pauseTheGame()
    }

    private fun switchEditMode() {
        editMode = !editMode
        if (editMode) {
            showSettings()
        } else {
            hideSettings()
        }
    }

    private fun showSettings() {
        gridDrawer.drawGrid()
        materialsContainer.visibility = View.VISIBLE
    }

    private fun hideSettings() {
        gridDrawer.removeGrid()
        materialsContainer.visibility = View.GONE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!gameCore.isPlaying()) {
            return super.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            KEYCODE_DPAD_UP -> onButtonPressed(UP)
            KEYCODE_DPAD_LEFT -> onButtonPressed(LEFT)
            KEYCODE_DPAD_DOWN -> onButtonPressed(BOTTOM)
            KEYCODE_DPAD_RIGHT -> onButtonPressed(RIGHT)
            KEYCODE_SPACE -> bulletDrawer.addNewBulletForTank(playerTank)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onButtonPressed(direction: Direction) {
        soundManager.tankMove()
        playerTank.move(direction, container, elementsDrawer.elementsOnContainer)
    }


    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!gameCore.isPlaying()) {
            return super.onKeyUp(keyCode, event)
        }
        when (keyCode) {
            KEYCODE_DPAD_UP, KEYCODE_DPAD_LEFT, KEYCODE_DPAD_DOWN, KEYCODE_DPAD_RIGHT -> onButtonReleased()
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun onButtonReleased() {
        if (enemyDrawer.tanks.isEmpty()) {
            soundManager.tankStop()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCORE_REQUEST_CODE) {
            recreate()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun showProgress() {
        container.visibility = View.GONE
        totalContainer.setBackgroundResource(R.color.gray)
        initTitle.visibility = View.VISIBLE
    }

    override fun dismissProgress() {
        container.visibility = View.VISIBLE
        totalContainer.setBackgroundResource(R.color.black)
        initTitle.visibility = View.GONE
        enemyDrawer.startEnemyCreation()
        soundManager.playIntroMusic()
        resumeTheGame()
    }
}
