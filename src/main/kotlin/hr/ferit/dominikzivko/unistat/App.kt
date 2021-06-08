package hr.ferit.dominikzivko.unistat

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.ui.UIManager
import hr.ferit.dominikzivko.unistat.web.AuthWebConnection
import hr.ferit.dominikzivko.unistat.web.WebConnection
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage
import org.apache.logging.log4j.LogManager
import org.koin.core.context.*
import org.koin.dsl.module
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JOptionPane
import kotlin.concurrent.thread
import kotlin.properties.Delegates
import kotlin.system.exitProcess

class App : Application() {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private val appRuntime: AppRuntime by lazy { GlobalContext.get().get() }

    override fun start(primaryStage: Stage) {
        log.info("Application started.")
        thread(name = "Starter Thread") {
            try {
                Platform.setImplicitExit(false)
                startKoin { modules(baseModule) }
                AppDatabase.initialize()
                appRuntime.start(primaryStage)
            } catch (t: Throwable) {
                log.fatal("Fatal error", t)
                Alerts.catching(strings["msg_error_occurred"], t)
                exit()
            }
        }
    }

    override fun stop() {
        log.info("Application stopping.")
        stopKoin()
        super.stop()
    }


    companion object {
        const val AUTHOR = "Altline"
        const val APPNAME = "UniStat"

        private val log by lazy { LogManager.getLogger(App::class.java) }

        val mainStylesheet: String by lazy {
            App::class.java.getResource("/gui/application.css").toExternalForm()
        }

        fun exit() {
            Platform.exit()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                initWorkDirectory()
                applock()
                launch(App::class.java, *args)
            } catch (t: Throwable) {
                log.fatal("Top level error", t)
                Alerts.catching(strings["msg_error_occurred"], t)
                exitProcess(ExitCodes.FAIL)
            }
        }

        private fun initWorkDirectory() {
            val appdata = System.getenv("APPDATA")
            val workDirPath: Path = if (!appdata.isNullOrBlank()) Paths.get(appdata, AUTHOR, APPNAME)
            else Paths.get(System.getProperty("user.home"), AUTHOR, APPNAME)
            System.setProperty("app.workdir", workDirPath.toString())
        }

        private fun applock() {
            try {
                val applockFile = Paths.get(System.getProperty("app.workdir"), "app.lock")
                val raf = RandomAccessFile(applockFile.toFile(), "rw")
                val channel = raf.channel
                val lock = channel?.tryLock()
                if (lock == null) {
                    log.warn("Unable to obtain app file lock - assuming another instance is already running. This instance will not launch.")
                    channel?.close()
                    raf.close()
                    JOptionPane.showMessageDialog(
                        null,
                        strings["error"],
                        strings["msg_alreadyRunning"],
                        JOptionPane.ERROR_MESSAGE
                    )
                    exitProcess(ExitCodes.ALREADY_RUNNING)
                }

                Runtime.getRuntime().addShutdownHook(object : Thread() {
                    override fun run() {
                        try {
                            lock.release()
                            channel.close()
                            raf.close()
                            Files.delete(applockFile)
                        } catch (e: IOException) {
                            log.error("", e)
                        }
                    }

                })
            } catch (t: Throwable) {
                log.fatal("Launch error", t)
                exitProcess(ExitCodes.FAIL)
            }
        }
    }
}

val baseModule = module {
    single { AppRuntime(get()) }
    single { UIManager() }
    single { Repository(get()) }
}

val remoteDatasourceModule = module {
    single { WebConnection() }
    single { AuthWebConnection(get()) }
    single<DataSource> { WebDataSource(get()) }
}

val localDatasourceModule = module {

}

object ExitCodes {
    const val FAIL = 1
    const val ALREADY_RUNNING = 10
}