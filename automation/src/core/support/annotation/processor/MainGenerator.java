/**
 * @author ehsan matean
 *
 */

package core.support.annotation.processor;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

import core.support.annotation.helper.FileCreatorHelper;
import core.support.annotation.helper.Logger;
import core.support.annotation.helper.annotationMap.DataMapHelper;
import core.support.annotation.helper.annotationMap.PanelMapHelper;
import core.support.annotation.helper.annotationMap.ServiceMapHelper;
import core.support.annotation.template.dataObject.CsvDataObject;
import core.support.annotation.template.dataObject.DataClass;
import core.support.annotation.template.dataObject.ModuleClass;
import core.support.annotation.template.manager.ModuleManager;
import core.support.annotation.template.manager.PanelManagerGenerator;
import core.support.annotation.template.service.Service;
import core.support.annotation.template.service.ServiceData;
import core.support.annotation.template.service.ServiceRunner;

@SupportedAnnotationTypes(value = { "core.support.annotation.Panel", "core.support.annotation.Data", "core.support.annotation.Service"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@AutoService(javax.annotation.processing.Processor.class)
public class MainGenerator extends AbstractProcessor {

	private static boolean isAnnotationRun = false;
	public static ProcessingEnvironment PROCESS_ENV;
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		if (!isAnnotationRun) {
			isAnnotationRun = true;

			PROCESS_ENV = processingEnv;

			System.out.println("Annotation called");
		
			
			// map of modules and class with @Panel annotation
			Map<String, List<Element>> appMap = PanelMapHelper.getPanelMap(roundEnv);
			
			// map of modules and classes with @Data annotation
			Map<String, List<Element>> dataObjectMap = DataMapHelper.getDataObjectMap(roundEnv);
			
			// map of modules and classes with @Data annotation
			Map<String, List<Element>> serviceObjectMap = ServiceMapHelper.getServiceObjectMap(roundEnv);
			
			for (Entry<String, List<Element>> entry : dataObjectMap.entrySet()) {
				Logger.debug("data object map: key " + entry.getKey());
				Logger.debug("data object map: value " + entry.getValue().get(0));

			}

			// generate managers
			PanelManagerGenerator.writePanelManagerClass(appMap);
			ModuleManager.writeModuleManagerClass(appMap);
			
			// generate data objects
			CsvDataObject.writeCsvDataClass();
			ModuleClass.writeModuleClass(dataObjectMap);
			DataClass.writeDataClass(dataObjectMap);
			
			// generate service objects
			Service.writeServiceClass();
			ServiceData.writeServiceDataClass();
			ServiceRunner.writeServiceClass(serviceObjectMap);
			
		    crateMarkerClass();
			
			System.out.println("Annotation generation complete");
		}
		return true;
	}
	
	/**
	 * a marker class is to indicate when the generated files have been created
	 * used for comparison with the class files.
	 * if class files are newer, than the marker class, then regenerate the code
	 */
	public static void crateMarkerClass() {
		try {
				JavaFileObject fileObject = FileCreatorHelper.createMarkerFile();

				BufferedWriter bw = new BufferedWriter(fileObject.openWriter());
				bw.append("/**Auto generated code,don't modify it. */ \n");
				bw.append("package marker;");
				bw.append("public class marker {}");
				bw.flush();
				bw.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}