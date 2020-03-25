package org.hy.common.hbase.plugins;

import java.util.Map;

import org.hy.common.Help;
import org.hy.common.app.Param;
import org.hy.common.xml.XJava;
import org.hy.common.xml.annotation.XType;
import org.hy.common.xml.annotation.Xjava;





/**
 * Http响应类型 ContentType
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-03-16
 * @version     v1.0
 */
@Xjava(XType.XML)
public class HttpContentType
{
    
    private static HttpContentType $HttpContentType;
    
    private static boolean         $Init = false;
    
    
    
    public synchronized static HttpContentType getInstance()
    {
        if ( $HttpContentType == null )
        {
            $HttpContentType = new HttpContentType();
        }
        
        return $HttpContentType;
    }
    
    
    
    /**
     * 按文件扩展名，查询及决定ContentType的类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_FilePostfix 文件扩展名(不带点)
     * @return
     */
    public static String getContentType(String i_FilePostfix)
    {
        return getInstance().findContentType(i_FilePostfix);
    }
    
    
    
    private HttpContentType()
    {
        if ( !$Init )
        {
            try
            {
                XJava.parserAnnotation(this.getClass().getName());
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
    }
    
    
    
    @SuppressWarnings("unchecked")
    public String findContentType(String i_FilePostfix)
    {
        // 设置方法1：getServletContext().getMimeType(filename)
        // 设置方法2：response.setContentType("multipart/form-data");  这样设置，会自动判断下载文件类型
        //          方法2的意思是把你表单的所有信息以流方式提交，页面上的所有信息已经都转换为了文件流，
        //          为的是能让服务端得到你上传的文件的文件流
        
        if ( Help.isNull(i_FilePostfix) )
        {
            return "application/octet-stream";
        }
        
        Param v_Param = ((Map<String ,Param>)XJava.getObject("HttpContentType")).get(i_FilePostfix.trim().toLowerCase());
        
        if ( v_Param == null )
        {
            return "application/octet-stream";
        }
        else
        {
            return Help.NVL(v_Param.getValue() ,"application/octet-stream");
        }
    }
    
}
