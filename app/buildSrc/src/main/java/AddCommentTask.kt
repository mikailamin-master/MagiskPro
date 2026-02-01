import com.android.apksig.ApkSigner
import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.dsl.ApkSigningConfig
import com.android.builder.internal.packaging.IncrementalPackager
import com.android.tools.build.apkzlib.zfile.ZFiles
import com.android.tools.build.apkzlib.zip.ZFileOptions
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.jar.JarFile

abstract class AddCommentTask : DefaultTask() {

    @get:Input
    abstract val comment: Property<String>

    @get:Input
    abstract val signingConfig: Property<ApkSigningConfig>

    @get:InputFiles
    abstract val apkFolder: DirectoryProperty

    @get:OutputDirectory
    abstract val outFolder: DirectoryProperty

    @get:Internal
    abstract val transformationRequest: Property<ArtifactTransformationRequest<AddCommentTask>>

    @TaskAction
    fun taskAction() =
        transformationRequest.get().submit(this) { artifact ->

            val inFile = File(artifact.outputFile)
            val tempFile = outFolder.file("unsigned_${inFile.name}").get().asFile
            val outFile = outFolder.file(inFile.name).get().asFile

            val zOptions = ZFileOptions().apply {
                noTimestamps = true
                autoSortFiles = true
            }

            outFile.parentFile.mkdirs()
            inFile.copyTo(tempFile, overwrite = true)

            // step 1: zip modify only
            ZFiles.apk(tempFile, zOptions).use {
                it.eocdComment = comment.get().toByteArray()
                it.get(IncrementalPackager.APP_METADATA_ENTRY_PATH)?.delete()
                it.get(IncrementalPackager.VERSION_CONTROL_INFO_ENTRY_PATH)?.delete()
                it.get(JarFile.MANIFEST_NAME)?.delete()
            }

            // step 2: v1 + v2 + v3 signing
            signApk(
                inputApk = tempFile,
                outputApk = outFile,
                signingConfig = signingConfig.get()
            )

            tempFile.delete()
            outFile
        }

    private fun signApk(
        inputApk: File,
        outputApk: File,
        signingConfig: ApkSigningConfig
    ) {
        val keyStore = KeyStore.getInstance(
            signingConfig.storeType ?: KeyStore.getDefaultType()
        )

        signingConfig.storeFile!!.inputStream().use {
            keyStore.load(it, signingConfig.storePassword!!.toCharArray())
        }

        val entry = keyStore.getEntry(
            signingConfig.keyAlias!!,
            KeyStore.PasswordProtection(signingConfig.keyPassword!!.toCharArray())
        ) as KeyStore.PrivateKeyEntry

        val cert = entry.certificate as X509Certificate

        val signerConfig = ApkSigner.SignerConfig.Builder(
            signingConfig.keyAlias!!,
            entry.privateKey,
            listOf(cert)
        ).build()

        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setV4SigningEnabled(false)
            .sign()
    }
}