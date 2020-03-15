/**
 * @author ehsan matean
 *
 */

package core.support.annotation.processor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import com.google.auto.service.AutoService;

import core.helpers.Helper;
import core.support.annotation.helper.FileCreatorHelper;
import core.support.annotation.helper.Logger;
import core.support.annotation.helper.annotationMap.AnnotationObject;
import core.support.annotation.helper.annotationMap.ModuleMapHelper;
import core.support.annotation.template.config.ConfigManager;
import core.support.annotation.template.config.ConfigVariableGenerator;
import core.support.annotation.template.dataObject.CsvDataObject;
import core.support.annotation.template.dataObject.DataClass;
import core.support.annotation.template.dataObject.ModuleClass;
import core.support.annotation.template.manager.ModuleManager;
import core.support.annotation.template.manager.PanelManagerGenerator;
import core.support.annotation.template.service.Service;
import core.support.annotation.template.service.ServiceData;
import core.support.annotation.template.service.ServiceRunner;
import core.support.configReader.PropertiesReader;

@SupportedAnnotationTypes(value = { "core.support.annotation.Panel", "core.support.annotation.Data", "core.support.annotation.Service" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@AutoService(javax.annotation.processing.Processor.class)
public class MainGenerator extends AbstractProcessor {

	private static boolean isAnnotationRun = false;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		return runAnnotation();
	}
	
	public static boolean runAnnotation() {
		if (!isAnnotationRun) {
			isAnnotationRun = true;

			Logger.debug("Annotation called");

			// map of modules and class with @Panel annotation
			AnnotationObject annotation = new AnnotationObject().panel();
			Map<String, List<String>> panelMap = ModuleMapHelper.getModuleMap(annotation);

			// map of modules and classes with @Data annotation
			annotation = new AnnotationObject().data();
			Map<String, List<String>> dataMap = ModuleMapHelper.getModuleMap(annotation);

			// map of modules and classes with @Service annotation
			annotation = new AnnotationObject().service();
			Map<String, List<String>> serviceMap = ModuleMapHelper.getModuleMap(annotation);

			// print out the map
			for (Entry<String, List<String>> entry : dataMap.entrySet()) {
				Logger.debug("module map: module: " + entry.getKey());
				Logger.debug("module map: paths: " + Arrays.toString(entry.getValue().toArray())) ;

			}

			// generate managers
			PanelManagerGenerator.writePanelManagerClass(panelMap);
			ModuleManager.writeModuleManagerClass(panelMap);

			// generate data objects
			CsvDataObject.writeCsvDataClass();
			ModuleClass.writeModuleClass(dataMap);
			DataClass.writeDataClass(dataMap);

			// generate service objects
			Service.writeServiceClass();
			ServiceData.writeServiceDataClass();
			ServiceRunner.writeServiceClass(serviceMap);

			// generate config objects
			ConfigManager.writeConfigManagerClass();
			ConfigVariableGenerator.writeConfigVariableClass();

			// create marker crasll
			createMarkerClass();

			System.out.println("Annotation generation complete");
		}
		return true;
	}
	

	/**
	 * a marker class is to indicate when the generated files have been created used
	 * for comparison with the class files. if class files are newer, than the
	 * marker class, then regenerate the code
	 */
	private static void createMarkerClass() {
		try {
			createFileList("src" + File.separator + "main", "src_dir", false);
			createFileList("resources" + File.separator + "api" + File.separator + "keywords", "src_dir", true);

			File file = FileCreatorHelper.createMarkerFile();
			
			FileWriter fw = new FileWriter(file);
		    BufferedWriter  bw = new BufferedWriter(fw);

			bw.append("/**Auto generated code,don't modify it. */ \n");
			bw.append("package marker;");
			bw.append("public class marker {}");
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * creates text file with the list of files in sourceDir used for file change
	 * detection
	 * 
	 * @param sourceDir
	 * @param fileName
	 */
	private static void createFileList(String sourceDir, String fileName, boolean isAppend) {
		File Directory = new File(Helper.getRootDir() + sourceDir);
		ArrayList<String> fileList = PropertiesReader.getAllFiles(Directory);
		String listString = String.join(",", fileList);
		if (isAppend)
			Helper.appendToFile("," + listString, "target/generated-sources", fileName, "txt");
		else
			Helper.writeFile(listString, "target/generated-sources", fileName, "txt");
	}

}