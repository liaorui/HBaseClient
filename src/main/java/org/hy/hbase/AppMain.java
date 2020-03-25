package org.hy.hbase;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.JFrame;

import org.hy.common.Help;
import org.hy.common.StringHelp;
import org.hy.common.app.AppParameter;
import org.hy.common.file.FileHelp;
import org.hy.common.hbase.HBase;
import org.hy.common.hbase.HBase.ResultType;
import org.hy.common.xml.XJava;





/**
 * 应用程序的启动入口
 * 
 * @author ZhengWei(HY)
 * @create 2014-05-19
 */
public class AppMain
{
    public  static final String $VersionNo  = "v1.7.0";
    
    public  static final String $SourceCode = "https://github.com/HY-ZhengWei/HBaseClient";  
    
    private static HBase        $HBase      = null;
    
    private static FileHelp     $FileHelp   = new FileHelp();
    
    
    
    public static void main(String[] i_Args) throws Exception 
    {
        Properties properties = new Properties();
        properties.load(AppMain.class.getResourceAsStream("/config.properties"));
        String hadoopUser = properties.getProperty("hadoop.user.name");
        if (!Help.isNull(hadoopUser)) {
            // 设置hbase用户
            System.setProperty("HADOOP_USER_NAME", hadoopUser);
        }

        AppParameter v_Apps     = new AppParameter(i_Args);
        String       v_FileName = v_Apps.getParamValue("file");
        String       v_FileType = v_Apps.getParamValue("fileType");
        String       v_CMD      = v_Apps.getParamValue("cmd");
        String       v_Language = Help.NVL(v_Apps.getParamValue("language") ,"cn");
        
        
        if ( v_Apps.isShowVersion() )
        {
            System.out.println(showVersionInfo());
            return;
        }
        
        
        if ( v_Apps.isShowHelp() )
        {
            System.out.println(showHelpInfo());
            return;
        }

        String v_HBaseIP = properties.getProperty("hbase.zookeeper.quorum");
        String v_znode = properties.getProperty("zookeeper.znode.parent");
        
        if ( Help.isNull(v_HBaseIP) )
        {
            System.out.println("Parameter [ip] is null.");
            return;
        }
        
        
        $HBase = new HBase(v_HBaseIP, v_znode);
        $HBase.setResultType(ResultType.ResultType_HData);
        
        
        if ( v_Apps.isExists("-window") )
        {
            XJava.putObject("SYS_LANGUAGE" ,v_Language);
            XJava.parserAnnotation("org.hy.hbase");
            AppFrame v_AppFrame = (AppFrame)XJava.getObject("AppFrame");
            
            Dimension v_Dimension = Toolkit.getDefaultToolkit().getScreenSize();
            v_Dimension.height = v_Dimension.height - 40;
                    
            v_AppFrame.setHBase($HBase);
            v_AppFrame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(v_AppFrame.getClass().getResource("img/HBase.png")));
            v_AppFrame.setSize(v_Dimension);
            v_AppFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            v_AppFrame.setTitle("HBase客户端 " + $VersionNo);
            v_AppFrame.setVisible(true);
            v_AppFrame.validate();
            v_AppFrame.init();
            
            return;
        }
        
        
        if ( Help.isNull(v_CMD) )
        {
            if ( Help.isNull(v_FileName) )
            {
                System.out.println("Parameter [file] is null.");
                return;
            }
            
            exeutesFile(new File(v_FileName) ,v_FileType);
        }
        else
        {
            executes(v_CMD);
        }
    }
    
    
    
    /**
     * 执行文件或目录下的文件批量put命令导入
     * 
     * @param i_File
     * @param i_FileType
     */
    public static void exeutesFile(File i_File ,String i_FileType)
    {
        if ( i_File.isFile() )
        {
            executes(i_File ,i_FileType);
        }
        else if ( i_File.isDirectory() )
        {
            File [] v_ChildFiles = i_File.listFiles();
            int     v_Count      = 0;
            
            for (int i=0; i<v_ChildFiles.length; i++)
            {
                File v_ChildFile = v_ChildFiles[i];
                
                if ( v_ChildFile.isFile() )
                {
                    System.out.println("\nExecute file " + (++v_Count) + " : [" + v_ChildFile.getAbsolutePath() + "] ...");
                    executes(v_ChildFile ,i_FileType);
                }
            }
        }
    }
    
    
    
    private static void executes(File i_File ,String i_FileType)
    {
        String v_FileType = i_FileType;
        
        if ( Help.isNull(v_FileType) )
        {
            v_FileType = getFileType(i_File);
        }
        
        if ( i_File.isFile() )
        {
            if ( !i_File.exists() )
            {
                System.out.println("File [" + i_File.getAbsolutePath() + "] is not exists.");
            }
            
            if ( !i_File.canRead() )
            {
                System.out.println("File [" + i_File.getAbsolutePath() + "] can not read.");
            }
            
            try
            {
                executes($FileHelp.getContent(i_File ,Help.NVL(v_FileType ,"UTF-8") ,true));
            }
            catch (Exception exce)
            {
                System.out.println("File [" + i_File.getAbsolutePath() + "] error:" + exce.getMessage());
            }
        }
    }
    
    
    
    /**
     * 批量执行命令（以回车分隔）
     * 
     * @author      ZhengWei(HY)
     * @version     v1.0
     *              v2.0  2018-05-23  1. 添加Create创建表命令的解释
     *                                2. 添加Drop删除表命令的解释
     *                                3. 添加Disable表失效命令的解释
     *                                4. 添加Truncate清空表命令的解释
     *                                5. 添加Delete删除列、删除列簇、删除行命令的解释
     *
     * @param i_Commands
     */
    public static void executes(String i_Commands)
    {
        String [] v_Commands = i_Commands.split("\n");
        int       v_Count    = 0;
        
        for (int i=0; i<v_Commands.length; i++)
        {
            String v_CmdData = v_Commands[i].trim();
            
            if ( Help.isNull(v_CmdData) )
            {
                continue;
            }
            
            String [] v_CmdDataArr = v_CmdData.split(" "); 
            if ( v_CmdDataArr.length < 2 )
            {
                continue;
            }
            
            String v_Cmd  = v_CmdDataArr[0].toLowerCase();
            String v_Data = v_CmdData.substring(v_Cmd.length() + 1).trim();
            
            // Put 命令的解释
            if ( "put".equals(v_Cmd) )
            {
                String [] v_Params = v_Data.split(",");
                String    v_Value  = "";
                
                if ( v_Params.length >= 4 )
                {
                    String v_TableName  = StringHelp.trim(v_Params[0].trim() ,"'");
                    String v_RowKey     = StringHelp.trim(v_Params[1].trim() ,"'");
                    String v_FamilyName = StringHelp.trim(v_Params[2].trim() ,"'").split(":")[0];
                    String v_ColumnName = StringHelp.trim(v_Params[2].trim() ,"'").split(":")[1];
                    
                    for (int x=3; x<v_Params.length; x++)
                    {
                        if ( x == 3)
                        {
                            v_Value = v_Params[x];
                        }
                        else
                        {
                            v_Value = v_Value + "," + v_Params[x];
                        }
                    }
                    v_Value = StringHelp.trim(v_Value.trim() ,";" ,"'");
                    
                    $HBase.update(v_TableName
                                 ,v_RowKey
                                 ,v_FamilyName
                                 ,v_ColumnName
                                 ,v_Value);
                    
                    System.out.println("-- " + StringHelp.rpad(++v_Count ,4 ," ") + ": " + v_TableName + "." + v_RowKey + "." + v_FamilyName + ":" + v_ColumnName + " = " + v_Value);
                }
                else if ( v_Params.length <= 1 )
                {
                    // Nothing 空白信息不处理
                }
                else
                {
                    System.out.println("\n-- Put Error: " + v_CmdData + "\n");
                }
            }
            // Create 创建表命令的解释
            else if ( "create".equals(v_Cmd) )
            {
                String [] v_Params = StringHelp.trim(v_Data ,";").replaceAll("'" ,"").split(",");
                
                if ( v_Params.length >= 2 )
                {
                    String v_TableName = v_Params[0];
                    $HBase.createTable(v_TableName ,Help.toList(v_Params).subList(1 ,v_Params.length));
                    
                    System.out.println("-- " + StringHelp.rpad(++v_Count ,4 ," ") + ": " + v_CmdData);
                }
                else
                {
                    System.out.println("\n-- Create Error: " + v_CmdData + "\n");
                }
            }
            // Drop 删除表命令的解释
            else if ( "drop".equals(v_Cmd) )
            {
                $HBase.dropTable(StringHelp.trim(v_Data ,";" ,"'"));
            }
            // Disable 表失效命令的解释
            else if ( "disable".equals(v_Cmd) )
            {
                $HBase.disableTable(StringHelp.trim(v_Data ,";" ,"'"));
            }
            // Truncate 清空表命令的解释
            else if ( "truncate".equals(v_Cmd) )
            {
                $HBase.disableTable(StringHelp.trim(v_Data ,";" ,"'"));
            }
            // Delete 删除列、删除列簇、删除行命令的解释
            else if ( "delete".equals(v_Cmd) )
            {
                String [] v_Params = StringHelp.trim(v_Data ,";").split(",");
                
                if ( v_Params.length >= 2 )
                {
                    String v_TableName = StringHelp.trim(v_Params[0] ,"'");
                    String v_RowKey    = StringHelp.trim(v_Params[1] ,"'");
                    
                    if ( v_Params.length >= 3 )
                    {
                        String [] v_FamilyColumn = StringHelp.trim(v_Params[2] ,"'").split(":");
                        
                        if ( v_FamilyColumn.length == 1 )
                        {
                            // 删除列簇
                            $HBase.delete(v_TableName ,v_RowKey ,v_FamilyColumn[0]);
                        }
                        else if ( v_FamilyColumn.length >= 2 )
                        {
                            // 删除列
                            $HBase.delete(v_TableName ,v_RowKey ,v_FamilyColumn[0] ,v_FamilyColumn[1]);
                        }
                    }
                    else
                    {
                        // 删除行
                        $HBase.delete(v_TableName ,v_RowKey);
                    }
                }
                else
                {
                    System.out.println("\n-- Delete Error: " + v_CmdData + "\n");
                }
            }
        }
    }
    
    
    
    /**
     * 简单判断是UTF-8或不是UTF-8，因为一般除了UTF-8之外就是GBK，所以就设置默认为GBK
     * 
     * 按照给定的字符集存储文件时，在文件的最开头的三个字节中就有可能存储着编码信息，
     * 所以，基本的原理就是只要读出文件前三个字节，判定这些字节的值，就可以得知其编码的格式。
     * 其实，如果项目运行的平台就是中文操作系统，如果这些文本文件在项目内产生，
     * 即开发人员可以控制文本的编码格式，只要判定两种常见的编码就可以了：GBK和UTF-8。
     * 由于中文Windows默认的编码是GBK，所以一般只要判定UTF-8编码格式。
     * 对于UTF-8编码格式的文本文件，其前3个字节的值就是-17、-69、-65，
     * 所以，判定是否是UTF-8编码格式的代码片段如下
     * @param i_File
     * @return
     */
    private static String getFileType(File i_File)
    {
        InputStream v_Input = null;
        byte []     v_Bytes = new byte[3];
        
        try
        {
            v_Input = new FileInputStream(i_File);
            v_Input.read(v_Bytes);
            
            if ( v_Bytes[0] == -17 && v_Bytes[1] == -69 && v_Bytes[2] == -65 )
            {
                return "UTF-8";
            }
            else
            {
                return "GBK";
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        finally
        {
            try
            {
                if ( v_Input != null )
                {
                    v_Input.close();
                }
            }
            catch (Exception exce)
            {
                // Nothing.
            }
        }
        
        return null;
    }
    
    
    
    private static String showVersionInfo()
    {
        return "\n" + $VersionNo + " \nHY.ZhengWei@qq.com\n"
             + "Support：" + $SourceCode + "\n";
    }
    
    
    
    private static String showHelpInfo()
    {
        StringBuilder v_Buffer = new StringBuilder();
        
        v_Buffer.append("\n");
        v_Buffer.append("HBaseClient -- 客户端工具");
        v_Buffer.append("\n\t").append("1: put支持中文");
        v_Buffer.append("\n\t").append("2: 支持文件形式的批量put命令执行");
        v_Buffer.append("\n\t").append("3: 支持扫描目录下所有文件的批量put命令执行");
        v_Buffer.append("\n\t").append("4: 支持put命令字符的执行");
        v_Buffer.append("\n\t").append("5: 支持文件编码自动识别");
        v_Buffer.append("\n\t").append("6: 支持图形化界面管理");
        v_Buffer.append("\n");
        v_Buffer.append("\n命令格式：");
        v_Buffer.append("\n\tHBaseClient <IP=xxx> <[File=xxx [FileType=UTF-8]] | [cmd=xxx]> [-window]");
        v_Buffer.append("\n");
        v_Buffer.append("\n命令参数说明：");
        v_Buffer.append("\n\t").append("IP         ").append("HBase数据库地址");
        v_Buffer.append("\n\t").append("File       ").append("加载命令文件的路径或目录的路径");
        v_Buffer.append("\n\t").append("FileType   ").append("加载命令文件的编码格式。可自动识别。当识别不到时，默认为UTF-8");
        v_Buffer.append("\n\t").append("CMD        ").append("加载命令字符");
        v_Buffer.append("\n\t").append("Language   ").append("选择语言。cn:简体中文、 en:English");
        v_Buffer.append("\n\t").append("-window    ").append("启动图形化管理界面");
        v_Buffer.append("\n\t").append("/v         ").append("显示版本信息");
        v_Buffer.append("\n\t").append("/?         ").append("显示帮助信息");
        v_Buffer.append("\n");
        
        return v_Buffer.toString();
    }
    
}
