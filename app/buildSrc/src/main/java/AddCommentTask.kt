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
    fun taskAction() = transformationRequest.get().submit(this) { artifact ->

        val inFile = File(artifact.outputFile)
        val tempFile = outFolder.file("unsigned_${inFile.name}").get().asFile
        val outFile = outFolder.file(inFile.name).get().asFile

        val options = ZFileOptions().apply {
            noTimestamps = true
            autoSortFiles = true
        }

        outFile.parentFile.mkdirs()
        inFile.copyTo(tempFile, overwrite = true)

        // step 1: zip modify only (NO signing here)
        ZFiles.apk(tempFile, options).use {
            it.eocdComment = comment.get().toByteArray()
            it.get(IncrementalPackager.APP_METADATA_ENTRY_PATH)?.delete()
            it.get(IncrementalPackager.VERSION_CONTROL_INFO_ENTRY_PATH)?.delete()
            it.get(JarFile.MANIFEST_NAME)?.delete()
        }

        // step 2: full signing with v1 + v2 + v3
        signWithV3(
            apkFile = tempFile,
            outFile = outFile,
            signingConfig = signingConfig.get()
        )

        tempFile.delete()
        outFile
    }

    private fun signWithV3(
        apkFile: File,
        outFile: File,
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

        val signerConfig = ApkSigner.SignerConfig.Builder(
            signingConfig.keyAlias!!,
            entry.privateKey,
            listOf(entry.certificate)
        ).build()

        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(apkFile)
            .setOutputApk(outFile)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setV4SigningEnabled(false)
            .build()
            .sign()
    }
}