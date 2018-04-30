package wxcheckutil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.entity.CDATA_Annotaion;
import com.entity.TextMessage;
import com.mysql.fabric.xmlrpc.base.Array;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;

import my.util.StringUtil;

public class XmlUtil {

	/**
	 * xml转map
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> xml2Map(HttpServletRequest request) throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		SAXReader reader = new SAXReader();
		InputStream is = null;
		Document doc = null;
		try {
			is = request.getInputStream();
			doc = reader.read(is);
			Element root = doc.getRootElement();
			List<Element> list = root.elements();
			for (Element e : list) {
				map.put(e.getName(), e.getText());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		} finally {
			is.close();
		}
		return map;
	}

	private static String cdata_prefix = "<![CDATA[";
	private static String cdata_suffix = "]]>";
	/**
	 * 文本消息对象转Xml
	 * 
	 * @param 对象
	 * @param 对象的类型
	 * @return
	 */
	public static String textMessage2Xml(Object object,Class<?> objectTargetClass) {
		XStream xStream = new XStream(new XppDriver() {
			/**
			 * 对xml标签里面值含有CDATA会被转义，比如<会被转义成&lt;
			 * 以下代码为了能加入不被转义的CDATA
			 */
			@Override
			public HierarchicalStreamWriter createWriter(Writer out) {
				PrettyPrintWriter ppw = new PrettyPrintWriter(out) {
					Boolean isCdata = false;
					Class<?> targetClazz = null;
					Class<?> superClass = null;
					List<String> super_class_field_names = new ArrayList<String>();
					List<String> target_class_field_names = new ArrayList<String>();

					@Override
					public void startNode(String name, @SuppressWarnings("rawtypes") Class clazz) {
						super.startNode(name, clazz);
						/**
						 * System.out.println(textMessage.getCreateTime()+",name:"+name+",clazz:"+clazz);
						 * 输出结果
						 * 173413984,name:xml,clazz:class com.entity.TextMessage
						 * 173413984,name:ToUserName,clazz:class java.lang.String
						 * 173413984,name:FromUserName,clazz:class java.lang.String
						 * 173413984,name:CreateTime,clazz:class java.lang.String
						 * 173413984,name:MsgType,clazz:class java.lang.String
						 * 173413984,name:Content,clazz:class java.lang.String
						 * 
						 * name为xml时，clazz为xStream.alias("xml", textMessage.getClass())声明的类
						 */
						try {
							if ("xml".equals(name)) {
								initParam(clazz);
							} else {
								if (super_class_field_names.contains(name)) {
									isCdata=superClass.getDeclaredField(StringUtil.firstLowerCase(name)).
											isAnnotationPresent(CDATA_Annotaion.class);
								} else if (target_class_field_names.contains(name)) {
									isCdata=targetClazz.getDeclaredField(StringUtil.firstLowerCase(name)).
											isAnnotationPresent(CDATA_Annotaion.class);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					private void initParam(@SuppressWarnings("rawtypes") Class clazz) {
						targetClazz = clazz;
						// 目标类属性名保存
						Field[] targetFields = targetClazz.getDeclaredFields();
						for (Field f : targetFields) {
							target_class_field_names.add(StringUtil.firstUpCase(f.getName()));
						}
						// 父类属性名保存到list中
						superClass = targetClazz.getSuperclass();
						if (!superClass.equals(Object.class)) {
							Field[] superFields = superClass.getDeclaredFields();
							for (Field f : superFields) {
								super_class_field_names.add(StringUtil.firstUpCase(f.getName()));
							}
						}
					}

					@Override
					protected void writeText(QuickWriter writer, String text) {
						if (isCdata) {
							writer.write(cdata_prefix + text + cdata_suffix);
						} else {
							writer.write(text);
						}
					}
				};
				return ppw;
			}

		});
		xStream.alias("xml", objectTargetClass);
		//开启注解转换（别名转换）
		xStream.processAnnotations(objectTargetClass);
		return xStream.toXML(object);
	}
}
