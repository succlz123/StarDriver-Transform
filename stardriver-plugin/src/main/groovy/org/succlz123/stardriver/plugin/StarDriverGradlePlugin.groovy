package org.succlz123.stardriver.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

import java.util.jar.JarEntry
import java.util.jar.JarFile

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES

class StarDriverGradlePlugin extends Transform implements Plugin<Project> {
    JarInput managerJarInput = null

    @Override
    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return "StarDriverPlugin"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        println '--- StarDriverPlugin transform start ---'
        def startTime = System.currentTimeMillis()
        if (outputProvider != null) {
            outputProvider.deleteAll()
        }
        List<ProcessClassNode> processClasses = new ArrayList<>()
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                getProcessClassFromDirectoryInput(directoryInput, processClasses, outputProvider)
            }
            input.jarInputs.each { JarInput jarInput ->
                getProcessClassFromJarInputs(jarInput, outputProvider)
            }
        }
        Map<String, ProcessClassNode> nodeHashMap = new HashMap<>()
        for (ProcessClassNode node : processClasses) {
            nodeHashMap.put(node.value.className, node)
        }
        for (ProcessClassNode node : processClasses) {
            ClassParameter currentParameter = node.value
            def values = currentParameter.annotationNode.values
            for (int i = 0; i < values.size(); i = i + 2) {
                String key = values[i]
                Object value = values[i + 1]
                if (key == "name") {
                    currentParameter.annotationValueName = (String) value
                } else if (key == "after") {
                    List<Type> types = (ArrayList<Type>) value
                    for (Type type : types) {
                        String className = getClassNameFromTypeName(type.valueBuffer)
                        ProcessClassNode beforeNode = nodeHashMap.get(className)
                        node.count++
                        beforeNode.next.add(node)
                    }
                } else if (key == "before") {
                    List<Type> types = (ArrayList<Type>) value
                    for (Type type : types) {
                        String className = getClassNameFromTypeName(type.valueBuffer)
                        ProcessClassNode afterNode = nodeHashMap.get(className)
                        afterNode.count++
                        node.next.add(afterNode)
                    }
                }
            }
        }
        List<ClassParameter> sortProcessClasses = new ArrayList<>(processClasses.size())
        Stack<ProcessClassNode> stack = new Stack<>()
        for (ProcessClassNode node : processClasses) {
            if (node.count == 0) {
                stack.push(node)
            }
        }
        while (!stack.isEmpty()) {
            ProcessClassNode processClassNode = stack.pop()
            if (processClassNode == null) {
                break
            }
            for (ProcessClassNode node : processClassNode.next) {
                node.count--
                if (node.count == 0) {
                    stack.push(node)
                }
            }
            sortProcessClasses.add(processClassNode.value)
        }
        if (sortProcessClasses.size() != processClasses.size()) {
            for (ClassParameter classParameter : sortProcessClasses) {
                nodeHashMap.remove(classParameter.className)
            }
            StringBuilder sb = new StringBuilder("The init tasks is Cycle, please check it carefully -> \n")
            def entries = nodeHashMap.entrySet()
            for (Map.Entry<String, ProcessClassNode> entry : entries) {
                sb.append(entry.getValue().value.className)
                sb.append("\n")
            }
            throw new IllegalStateException(sb.toString())
        }
        if (managerJarInput != null && sortProcessClasses.size() > 0) {
            handleManagerJarInputs(managerJarInput, sortProcessClasses, outputProvider)
        }
        def cost = (System.currentTimeMillis() - startTime) / 1000d
        println '--- StarDriverPlugin transform end - cost ' + cost + 's ---'
    }

    void getProcessClassFromDirectoryInput(DirectoryInput dirInput, List<ProcessClassNode> processClasses, TransformOutputProvider outputProvider) {
        if (dirInput.file.isDirectory()) {
            dirInput.file.eachFileRecurse { File file ->
                def name = file.name
                if (isProcessClass(name)) {
                    ClassParameter parameter = processStarDriverAnnotation(file.bytes)
                    if (parameter != null) {
                        println '--- task class from "class" ' + parameter.className + ' ---'
                        parameter.parentFilePath = dirInput.getFile().getAbsolutePath()
                        ProcessClassNode node = new ProcessClassNode(parameter)
                        processClasses.add(node)
                    }
                }
            }
        }
        def dest = outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes,
                dirInput.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(dirInput.file, dest)
    }

    void getProcessClassFromJarInputs(JarInput jarInput, TransformOutputProvider outputProvider) {
        boolean isManagerJar = false
        def jarName = jarInput.name
        def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4)
        }
        if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
//                InputStream inputStream = jarFile.getInputStream(jarEntry)
                if (isProcessClass(entryName)) {
                    if (isManagerClass(entryName)) {
                        println '--- manager class from "jar" ' + entryName + ' ---'
                        managerJarInput = jarInput
                        isManagerJar = true
                    }
                }
            }
        }
        if (!isManagerJar) {
            def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(jarInput.file, dest)
        }
    }

    static void handleManagerJarInputs(JarInput jarInput, List<ClassParameter> processClasses, TransformOutputProvider outputProvider) {
        String path = jarInput.file.absolutePath
        File jarFile = new File(path)
        String jarZipDir = jarFile.getParent() + "/" + jarFile.getName().replace('.jar', '')
        // xxx.xxx.xxx.class xxx.xxx.yyy.class
        List classNameList = JarZipUtil.unzipJar(path, jarZipDir)
        // delete the original jar package.
        jarFile.delete()

        ClassPool pool = ClassPool.getDefault()
        pool.appendClassPath(jarZipDir)
        String user = System.getProperties().getProperty("user.home")
        // add you android sdk directory here
        pool.appendClassPath(user + "/Library/Android/sdk/platforms/android-27/android.jar")
        for (ClassParameter classParameter : processClasses) {
            pool.appendClassPath(classParameter.parentFilePath)
        }
        println '--- process manager class - resort list ---'
        for (String className : classNameList) {
            if (className.endsWith("StarDriverManager.class")) {
                className = className.substring(0, className.length() - 6)
                CtClass c = pool.getCtClass(className)
                if (c.isFrozen()) {
                    c.defrost()
                }
                CtMethod method = c.getDeclaredMethod("initTasks")
                StringBuilder sb = new StringBuilder()
                sb.append("if (context == null) { throw new IllegalStateException(\"The application is null\"); }")
                sb.append("\n")
                sb.append("long now = 0L;")
                sb.append("\n")
                sb.append("now = System.currentTimeMillis();")
                sb.append("\n")
                for (int i = 0; i < processClasses.size(); i++) {
                    ClassParameter classParameter = processClasses.get(i)
                    String taskClassName = classParameter.className
                    println '--- class - ' + classParameter.className + ' ---'
                    String[] instanceNames = taskClassName.split("\\.")
                    String instanceName = instanceNames[instanceNames.length - 1]
                    sb.append(taskClassName + " " + instanceName + " = new " + taskClassName + "();")
                    sb.append("\n")
                    String resultName = instanceName + "_result"
                    sb.append("org.succlz123.stardriver.lib.StarDriverStatistics " + resultName + " = new org.succlz123.stardriver.lib.StarDriverStatistics();")
                    sb.append("\n")
                    sb.append(instanceName + ".initialize(context, " + resultName + ");")
                    sb.append("\n")
                    sb.append(resultName + ".useTime = System.currentTimeMillis() - now;")
                    sb.append("\n")
                    sb.append(resultName + ".name = \"" + classParameter.annotationValueName + "\";")
                    sb.append("\n")
                    sb.append(resultName + ".className = \"" + taskClassName + "\";")
                    sb.append("\n")
                    sb.append("statistics.add(" + resultName + ");")
                    sb.append("\n")
                    if (i != processClasses.size() - 1) {
                        sb.append("now = System.currentTimeMillis();")
                    }
                }
                method.insertAfter(sb.toString())
                c.writeFile(jarZipDir)
                c.detach()
            }
        }
        JarZipUtil.zipJar(jarZipDir, path)
        //        FileUtils.deleteDirectory(new File(jarZipDir))
        def dest = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        FileUtils.copyFile(jarInput.file, dest)
    }

    private static ClassParameter processStarDriverAnnotation(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes)
        ClassNode cn = new ClassNode()
        classReader.accept(cn, EXPAND_FRAMES)
        List<AnnotationNode> annotations = cn.invisibleAnnotations;
        if (annotations != null) {
            for (AnnotationNode an : annotations) {
                if (an.desc.contains("StarDriverInit")) {
                    ClassParameter parameter = new ClassParameter()
                    parameter.annotationNode = an
                    parameter.className = covertName(cn.name)
                    parameter.annotationName = getClassNameFromTypeName(an.desc)
                    return parameter
                }
            }
        }
        return null
    }

    // xxx/yyy/zzz -> xxx.yyy.zzz
    private static String covertName(String name) {
        return name.replace('\\', '.').replace('/', '.')
    }

    private static String getClassNameFromTypeName(String typeName) {
        String name = covertName(typeName)
        return covertName(typeName).substring(1, name.length() - 1)
    }

    private static boolean isProcessClass(String name) {
        return name.endsWith(".class") && !name.startsWith("R\$") && "R.class" != name && "BuildConfig.class" != name
    }

    private static boolean isManagerClass(String name) {
        return name.contains("StarDriverManager.class")
    }
}
