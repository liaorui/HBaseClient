package org.hy.common.hbase.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hy.common.ByteHelp;
import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.StringHelp;
import org.hy.common.hbase.HBase;
import org.hy.common.hbase.HData;





/**
 * 用HBase数据库保存大文件的相关操作 
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-03-14
 * @version     v1.0
 */
public class HBigFile
{
    
    /** 访问的HBase数据库 */
    private HBase       hbase;
    
    /** 大文件信息 */
    private BigFileInfo bigFileInfo;
    
    /** 大文件数据 */
    private BigFileData bigFileData;
    
    /** 分割序号的最大长度。默认为 6 位。这个属性并不实质性的影响任何操作。只影响序号前缀补0的个数 */
    private int         segmentMaxLen;
    
    
    
    /**
     * 构造器 
     *
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @param i_HBase   访问的HBase数据库
     * @param i_BFInfo  大文件信息
     * @param i_BFData  大文件数据
     */
    public HBigFile(HBase i_HBase ,BigFileInfo i_BFInfo ,BigFileData i_BFData)
    {
        if ( i_HBase == null )
        {
            throw new VerifyError("HBase is null.");
        }
        
        if ( i_BFInfo == null )
        {
            throw new VerifyError("BigFileInfo is null.");
        }
        
        if ( i_BFData == null )
        {
            throw new VerifyError("BigFileData is null.");
        }
        
        this.hbase         = i_HBase;
        this.bigFileInfo   = i_BFInfo;
        this.bigFileData   = i_BFData;
        this.segmentMaxLen = 6;
        
        this.init();
    }
    
    
    
    /**
     * 初始数据库表信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     */
    public synchronized void init()
    {
        if ( !this.hbase.isExistsTable(this.bigFileInfo.getTableName()) )
        {
            this.hbase.createTable(this.bigFileInfo.getTableName() 
                                  ,this.bigFileInfo.getFamilyName());
        }
        
        if ( !this.hbase.isExistsTable(this.bigFileData.getTableName()) )
        {
            this.hbase.createTable(this.bigFileData.getTableName() 
                                  ,this.bigFileData.getFamilyName() 
                                  ,this.getBlockSize());
        }
    }
    
    
    
    /**
     * 按时间顺序生成行主键
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @return
     */
    protected synchronized String makeID_Time()
    {
        Date   v_Date   = Date.getNowTime();
        Random v_Random = new Random();
        
        return v_Date.getFullMilli_ID() + StringHelp.lpad(v_Random.nextInt(1000) ,4 ,"0");
    }
    
    
    
    /**
     * 获取字节单位的数据块大小
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @return
     */
    protected int getBlockSize()
    {
        return this.bigFileData.getFamilyBlockSize() * 1024;
    }
    
    
    
    /**
     * 生成分割的序号
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_SegmentNo
     * @return
     */
    protected String makeSegmentNo(int i_SegmentNo)
    {
        return StringHelp.lpad(i_SegmentNo ,this.segmentMaxLen ,"0");
    }
    
    
    
    /**
     * 保存文件基本信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @param i_RowKey       行主键
     * @param i_FileName     文件名称
     * @param i_FileSize     文件大小
     * @param i_FileType     文件类型。由外界决定输入。可为空
     * @param i_SegmentSize  分割段的大小
     * @param i_ElseDatas    其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     */
    protected void putFileInfo(String              i_RowKey 
                              ,String              i_FileName 
                              ,long                i_FileSize 
                              ,String              i_FileType 
                              ,int                 i_SegmentSize
                              ,Map<String ,Object> i_ElseDatas)
    {
        List<HData> v_HDatas   = new ArrayList<HData>();
        String      v_FileType = Help.NVL(i_FileType ,Help.NVL(StringHelp.getFilePostfix(i_FileName)).replaceAll("\\." ,"")).toLowerCase();
        
        v_HDatas.add(new HData(i_RowKey ,this.bigFileInfo.getColumn_ID()          ,i_RowKey)                        .setFamilyName(this.bigFileInfo.getFamilyName()));
        v_HDatas.add(new HData(i_RowKey ,this.bigFileInfo.getColumn_FileName()    ,i_FileName)                      .setFamilyName(this.bigFileInfo.getFamilyName()));
        v_HDatas.add(new HData(i_RowKey ,this.bigFileInfo.getColumn_FileSize()    ,String.valueOf(i_FileSize))      .setFamilyName(this.bigFileInfo.getFamilyName()));
        v_HDatas.add(new HData(i_RowKey ,this.bigFileInfo.getColumn_FileType()    ,v_FileType)                      .setFamilyName(this.bigFileInfo.getFamilyName()));
        v_HDatas.add(new HData(i_RowKey ,this.bigFileInfo.getColumn_SegmentSize() ,String.valueOf(i_SegmentSize))   .setFamilyName(this.bigFileInfo.getFamilyName()));
        v_HDatas.add(new HData(i_RowKey ,this.bigFileInfo.getColumn_CreateTime()  ,Date.getNowTime().getFullMilli()).setFamilyName(this.bigFileInfo.getFamilyName()));
        
        v_HDatas.addAll(HData.toHDatas(i_RowKey ,this.bigFileInfo.getFamilyName() ,i_ElseDatas));
        
        this.hbase.put(this.getBigFileInfo().getTableName() ,v_HDatas);
    }
    
    
    
    /**
     * 将文件数据(分割后的一小段数据)保存到HBase数据库中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @param i_RowKey     行主键
     * @param i_SegmentNo  分割编号。最小下标从 1 开始
     * @param i_Data       分割后的其中一小段数据
     */
    protected void putFileData(String i_RowKey ,int i_SegmentNo ,String i_Data)
    {
        this.hbase.put(this.bigFileData.getTableName() 
                      ,i_RowKey + "_" + this.makeSegmentNo(i_SegmentNo)
                      ,this.bigFileData.getFamilyName() 
                      ,this.makeSegmentNo(i_SegmentNo)
                      ,i_Data);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_File       文件对象
     * @return
     * @throws IOException
     */
    public String write(File i_File) throws IOException
    {
        return this.write(null ,i_File ,null ,null);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @param i_File       文件对象
     * @param i_ElseDatas  其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     * @return
     * @throws IOException
     */
    public String write(File i_File ,Map<String ,Object> i_ElseDatas) throws IOException
    {
        return this.write(null ,i_File ,null ,i_ElseDatas);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_File       文件对象
     * @param i_FileType   文件类型。由外界决定输入。可为空
     * @return
     * @throws IOException
     */
    public String write(File i_File ,String i_FileType) throws IOException
    {
        return this.write(null ,i_File ,i_FileType ,null);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @param i_File       文件对象
     * @param i_FileType   文件类型。由外界决定输入。可为空
     * @param i_ElseDatas  其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     * @return
     * @throws IOException
     */
    public String write(File i_File ,String i_FileType ,Map<String ,Object> i_ElseDatas) throws IOException
    {
        return this.write(null ,i_File ,i_FileType ,i_ElseDatas);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_FileName   文件名称
     * @param i_File       文件对象
     * @return
     * @throws IOException
     */
    public String write(String i_FileName ,File i_File) throws IOException
    {
        return this.write(i_FileName ,i_File ,null ,null);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @param i_FileName   文件名称
     * @param i_File       文件对象
     * @param i_ElseDatas  其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     * @return
     * @throws IOException
     */
    public String write(String i_FileName ,File i_File ,Map<String ,Object> i_ElseDatas) throws IOException
    {
        return this.write(i_FileName ,i_File ,null ,i_ElseDatas);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_FileName   文件名称。可为空，当为空时，取 i_File.getName() 的值
     * @param i_File       文件对象
     * @param i_FileType   文件类型。由外界决定输入。可为空
     * @return
     * @throws IOException
     */
    public String write(String i_FileName ,File i_File ,String i_FileType) throws IOException
    {
        return this.write(i_FileName ,i_File ,i_FileType ,null);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @param i_FileName   文件名称。可为空，当为空时，取 i_File.getName() 的值
     * @param i_File       文件对象
     * @param i_FileType   文件类型。由外界决定输入。可为空
     * @param i_ElseDatas  其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     * @return
     * @throws IOException
     */
    public String write(String i_FileName ,File i_File ,String i_FileType ,Map<String ,Object> i_ElseDatas) throws IOException
    {
        if ( i_File == null )
        {
            throw new NullPointerException("File is null.");
        }
        
        if ( !i_File.isFile() )
        {
            throw new VerifyError("File object[" + i_File.getAbsolutePath() + "] is not file.");
        }
        
        if ( !i_File.canRead() )
        {
            throw new VerifyError("File[" + i_File.getAbsolutePath() + "] can not read.");
        }
        
        
        try
        {
            return this.write(Help.NVL(i_FileName ,i_File.getName()) 
                             ,new FileInputStream(i_File)
                             ,i_FileType
                             ,i_ElseDatas);
        }
        catch (Exception exce)
        {
            throw new IOException("Write file error: " + exce.getMessage());
        }
        finally
        {
            // v_Input已在内部方法中关闭，无在些关闭了。
        }
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_FileName     文件名称
     * @param i_SourceInput  输入流。内部有"关闭"操作
     * @return               保存成功时，返回数据记录的行主键
     * @throws IOException
     */
    public String write(String i_FileName ,InputStream i_SourceInput) throws IOException
    {
        return this.write(i_FileName ,i_SourceInput ,null ,null);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @param i_FileName     文件名称
     * @param i_SourceInput  输入流。内部有"关闭"操作
     * @param i_ElseDatas    其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     * @return               保存成功时，返回数据记录的行主键
     * @throws IOException
     */
    public String write(String i_FileName ,InputStream i_SourceInput ,Map<String ,Object> i_ElseDatas) throws IOException
    {
        return this.write(i_FileName ,i_SourceInput ,null ,i_ElseDatas);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @param i_FileName     文件名称
     * @param i_SourceInput  输入流。内部有"关闭"操作
     * @param i_FileType     文件类型。由外界决定输入。可为空
     * @return               保存成功时，返回数据记录的行主键
     * @throws IOException
     */
    public String write(String i_FileName ,InputStream i_SourceInput ,String i_FileType) throws IOException
    {
        return this.write(i_FileName ,i_SourceInput ,i_FileType ,null);
    }
    
    
    
    /**
     * 将文件保存在HBase数据库。
     * 
     * 当文件过大时，为被分割为多个小段分别保存在HBase数据库中的多个行字段中。
     * 字段的名称，就是分割段的序号。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @param i_FileName     文件名称
     * @param i_SourceInput  输入流。内部有"关闭"操作
     * @param i_FileType     文件类型。由外界决定输入。可为空
     * @param i_ElseDatas    其它数据信息。可为空。Map.key，即为字段名称，Map.value即为字段值
     * @return               保存成功时，返回数据记录的行主键
     * @throws IOException
     */
    public String write(String i_FileName ,InputStream i_SourceInput ,String i_FileType ,Map<String ,Object> i_ElseDatas) throws IOException
    {
        if ( Help.isNull(i_FileName) )
        {
            throw new NullPointerException("FileName is null.");
        }
        
        if ( i_SourceInput == null )
        {
            throw new NullPointerException("Source inputstream is null.");
        }
        
        int     v_BufferSize = this.getBlockSize();
        byte [] v_ReadBuffer = new byte[v_BufferSize];
        int     v_ReadSize   = 0;
        long    v_FileSize   = 0L;
        int     v_SegmentNo  = 0;
        String  v_RowKey     = this.makeID_Time();
        
        try
        {
            while ( (v_ReadSize = i_SourceInput.read(v_ReadBuffer ,0 ,v_BufferSize)) > 0 )
            {
                String v_Data = StringHelp.bytesToHex(v_ReadBuffer ,0 ,v_ReadSize);
                
                this.putFileData(v_RowKey ,++v_SegmentNo ,v_Data);
                v_FileSize += v_ReadSize;
            }
            
            this.putFileInfo(v_RowKey ,i_FileName ,v_FileSize ,i_FileType ,v_SegmentNo ,i_ElseDatas);
            
            return v_RowKey;
        }
        catch (Exception exce)
        {
            throw new IOException(exce.getMessage());
        }
        finally
        {
            if ( i_SourceInput != null )
            {
                try
                {
                    i_SourceInput.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
            i_SourceInput = null;
        }
    }
    
    
    
    /**
     * 读取文件基本信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @param i_RowKey  行主键
     * @return
     */
    public HBFile readFileInfo(String i_RowKey)
    {
        Map<String ,Object> v_Row = this.hbase.getRow(this.bigFileInfo.getTableName() ,i_RowKey);
        HBFile              v_Ret = new HBFile();
        
        v_Ret.setId(i_RowKey);
        v_Ret.setFileName(                    v_Row.get(this.bigFileInfo.getColumn_FileName())   .toString());
        v_Ret.setFileSize(   Long.parseLong(  v_Row.get(this.bigFileInfo.getColumn_FileSize())   .toString()));
        v_Ret.setFileType(                    v_Row.get(this.bigFileInfo.getColumn_FileType())   .toString());
        v_Ret.setSegmentSize(Integer.parseInt(v_Row.get(this.bigFileInfo.getColumn_SegmentSize()).toString()));
        v_Ret.setCreateTime(         new Date(v_Row.get(this.bigFileInfo.getColumn_CreateTime()).toString()));
        
        v_Row.remove(this.bigFileInfo.getColumn_ID());
        v_Row.remove(this.bigFileInfo.getColumn_FileName());
        v_Row.remove(this.bigFileInfo.getColumn_FileSize());
        v_Row.remove(this.bigFileInfo.getColumn_FileType());
        v_Row.remove(this.bigFileInfo.getColumn_SegmentSize());
        v_Row.remove(this.bigFileInfo.getColumn_CreateTime());
        
        v_Ret.setElseDatas(v_Row);
        
        return v_Ret;
    }
    
    
    
    /**
     * 读取分段的数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_RowKey     行主键
     * @param i_SegmentNo  分割编号。最小下标从 1 开始
     * @return
     */
    public byte [] read(String i_RowKey ,int i_SegmentNo)
    {
        Object v_Value = this.hbase.getValue(this.bigFileData.getTableName() 
                                            ,i_RowKey + "_" + this.makeSegmentNo(i_SegmentNo)
                                            ,this.bigFileData.getFamilyName() 
                                            ,this.makeSegmentNo(i_SegmentNo));
        
        if ( v_Value != null )
        {
            return StringHelp.hexToBytes(v_Value.toString());
        }
        else
        {
            return new byte[0];
        }
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入到本地文件中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_RowKey        行主键
     * @param i_SavePath      保存目录或保存文件
     *                        当为保存目录时，取数据库中的文件名。
     * @param i_IsOver        是否覆盖。当文件已存在时的判定
     * @return                返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToFile(String  i_RowKey 
                            ,String  i_SavePath 
                            ,boolean i_IsOver) throws IOException
    {
        return this.readToFile(i_RowKey ,new File(i_SavePath) ,i_IsOver);
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入到本地文件中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_RowKey        行主键
     * @param i_SavePath      保存目录
     * @param i_SaveFileName  保存文件名称。可为空，当为空时，取数据库中的文件名。
     * @param i_IsOver        是否覆盖。当文件已存在时的判定
     * @return                返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToFile(String  i_RowKey 
                            ,String  i_SavePath 
                            ,String  i_SaveFileName 
                            ,boolean i_IsOver) throws IOException
    {
        if ( Help.isNull(i_SavePath) )
        {
            throw new NullPointerException("SavePath is null.");
        }
        
        File v_SavePath = new File(i_SavePath.trim());
        if ( !v_SavePath.isDirectory() )
        {
            throw new VerifyError("SavePath is not Directory.");
        }
        
        if ( Help.isNull(i_SaveFileName) )
        {
            return this.readToFile(i_RowKey ,v_SavePath ,i_IsOver);
        }
        else
        {
            File v_SaveFile = new File(v_SavePath.getAbsolutePath() + Help.getSysPathSeparator() + i_SaveFileName.trim());
            return this.readToFile(i_RowKey ,v_SaveFile ,i_IsOver);
        }
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入到本地文件中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_RowKey        行主键
     * @param i_SaveFile      保存目录或是保存文件
     *                        当为保存目录时，取数据库中的文件名
     * @param i_IsOver        是否覆盖。当文件已存在时的判定
     * @return                返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToFile(String i_RowKey ,File i_SaveFile ,boolean i_IsOver) throws IOException
    {
        if ( i_SaveFile == null )
        {
            throw new NullPointerException("SaveFile is null.");
        }
        
        HBFile v_HBFile = this.readFileInfo(i_RowKey);
        if ( v_HBFile == null )
        {
            throw new NullPointerException("HBFile is null.");
        }
        
        File v_SaveFile = null;
        if ( i_SaveFile.isDirectory() )
        {
            v_SaveFile = new File(i_SaveFile.getAbsolutePath() + Help.getSysPathSeparator() + v_HBFile.getFileName());
        }
        else
        {
            v_SaveFile = i_SaveFile;
        }
        
        if ( v_SaveFile.exists() )
        {
            if ( i_IsOver )
            {
                v_SaveFile.delete();
            }
            else
            {
                throw new VerifyError("SaveFile[" + v_SaveFile.getAbsolutePath() + "] is exists.");
            }
        }
        
        FileOutputStream v_Output = null;
        try
        {
            v_Output = new FileOutputStream(v_SaveFile);
            return this.readToStream(v_HBFile ,v_Output ,false);
        }
        catch (Exception exce)
        {
            throw new IOException("ReadToFile is error: " + exce.getMessage());
        }
        finally
        {
            if ( v_Output != null )
            {
                try
                {
                    v_Output.close();
                }
                catch (Exception exce)
                {
                    exce.printStackTrace();
                }
            }
        }
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入(下载)到Http响应IO流(客户端)中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-17
     * @version     v1.0
     *
     * @param i_RowKey       行主键
     * @param io_Response    Http响应对象
     * @return               返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToHttp(String i_RowKey ,HttpServletResponse io_Response) throws IOException
    {
        return this.readToHttp(i_RowKey ,null ,io_Response);
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入(下载)到Http响应IO流(客户端)中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-17
     * @version     v1.0
     *
     * @param i_HBFile       大文件信息对象
     * @param io_Response    Http响应对象
     * @return               返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToHttp(HBFile i_HBFile ,HttpServletResponse io_Response) throws IOException
    {
        return this.readToHttp(i_HBFile ,null ,io_Response);
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入(下载)到Http响应IO流(客户端)中。
     * 
     * 支持客户端请求的断点续传功能
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_RowKey       行主键
     * @param i_Request      Http请求对象。可为空，当为空时，不支持断点续传功能
     * @param io_Response    Http响应对象
     * @return               返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToHttp(String i_RowKey ,HttpServletRequest i_Request ,HttpServletResponse io_Response) throws IOException
    {
        HBFile v_HBFile = this.readFileInfo(i_RowKey);
        
        return this.readToHttp(v_HBFile ,i_Request ,io_Response);
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入(下载)到Http响应IO流(客户端)中。
     * 
     * 支持客户端请求的断点续传功能
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_HBFile       大文件信息对象
     * @param i_Request      Http请求对象。可为空，当为空时，不支持断点续传功能
     * @param io_Response    Http响应对象
     * @return               返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToHttp(HBFile i_HBFile ,HttpServletRequest i_Request ,HttpServletResponse io_Response) throws IOException
    {
        String v_ContentType = HttpContentType.getContentType(i_HBFile.getFileName());
        
        // 非常重要
        io_Response.reset();
        io_Response.setHeader("Content-Disposition" ,"attachment;filename=" + i_HBFile.getFileName());
        io_Response.setHeader("Cache-Control"       ,"no-store");
        io_Response.setHeader("Pragma"              ,"no-cache");
        io_Response.setContentType(v_ContentType);  
        
        String  v_Range       = i_Request == null ? null : i_Request.getHeader("Range");
        boolean v_IsRange     = false;                          // 标记是否断点续传
        long    v_FileLength  = i_HBFile.getFileSize();
        long    v_ContentLen  = 0;
        long    v_Place_Being = 0;
        long    v_Place_End   = 0;
        
        // 判断客户端是否请求续传
        if ( v_Range != null && v_Range.trim().length() > 0 && !"null".equals(v_Range) )
        {
            v_IsRange = true;
            io_Response.setHeader("Accept-Ranges" ,"bytes");                   // 同意客户端的续传请求
            io_Response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            
            String v_RangeBytes = v_Range.replaceAll("bytes=" ,"");
            
            if ( v_RangeBytes.startsWith("-") )
            {
                // 表示最后500个字节：Range: bytes=-500
                v_Place_End   = Long.parseLong(v_RangeBytes.substring(v_RangeBytes.indexOf("-") + 1));
                v_Place_Being = v_FileLength - v_Place_End - 1;
            }
            else if ( v_RangeBytes.endsWith("-") )
            {
                // 表示500字节以后的范围：Range: bytes=500-
                v_Place_Being = Long.parseLong(v_RangeBytes.substring(0 ,v_RangeBytes.indexOf("-")));
                v_Place_End   = v_FileLength - v_Place_Being - 1;
            }
            else
            {
                // 表示头500个字节：  Range: bytes=0-499
                // 表示第二个500字节：Range: bytes=500-999
                v_Place_Being = Long.parseLong(v_RangeBytes.substring(0 ,v_RangeBytes.indexOf("-")));
                v_Place_End   = Long.parseLong(v_RangeBytes.substring(v_RangeBytes.indexOf("-") + 1));
            }
        }
        // 没有续传的情况
        else
        {
            v_Place_Being = 0;
            v_Place_End   = v_FileLength - 1;
        }
        
        
        v_ContentLen = v_Place_End - v_Place_Being + 1;
        io_Response.setHeader("Content-Length" ,v_ContentLen + "");
        
        
        if ( v_IsRange )
        {
            // 断点开始，响应的格式是: Content-Range: bytes [文件块的开始字节]-[文件块的结束字节]/[文件的总大小]
            String v_ContentRange = new StringBuilder("bytes ").append(v_Place_Being).append("-").append(v_Place_End).append("/").append(v_FileLength).toString();
            io_Response.setHeader("Content-Range" ,v_ContentRange);
            
            int          v_SegmentNo    = (int)Math.floor(v_Place_Being / this.getBlockSize()) + 1;
            int          v_SegmentBeing = (int)(v_Place_Being % this.getBlockSize());
            byte []      v_Data         = ByteHelp.substr(this.read(i_HBFile.getId() ,v_SegmentNo) ,v_SegmentBeing ,(int)v_ContentLen);
            OutputStream v_Output       = io_Response.getOutputStream();
            
            try
            {
                v_Output.write(v_Data);
                
                if ( v_Data.length < v_ContentLen )
                {
                    if ( v_SegmentNo < i_HBFile.getSegmentSize().intValue() )
                    {
                        v_Data = ByteHelp.substr(this.read(i_HBFile.getId() ,v_SegmentNo + 1) ,0 ,(int)(v_ContentLen - v_Data.length));
                        v_Output.write(v_Data);
                    }
                }
                
                v_Output.flush();
                return i_HBFile;
            }
            catch (Exception exce)
            {
                throw new IOException("readToHttp is error. " + exce.getMessage());
            }
            finally
            {
                if ( v_Output != null )
                {
                    try
                    {
                        v_Output.close();
                    }
                    catch (Exception exce)
                    {
                        exce.printStackTrace();
                    }
                }
            }
        }
        else
        {
            return this.readToStream(i_HBFile ,io_Response.getOutputStream());
        }
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入到IO流中。
     * 
     * 注：内部自动关闭IO流
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_RowKey       行主键
     * @param io_Out         IO流
     * @return               返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToStream(String i_RowKey ,OutputStream io_Out) throws IOException
    {
        return this.readToStream(i_RowKey ,io_Out ,true);
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入到IO流中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_RowKey       行主键
     * @param io_Out         IO流
     * @param i_IsCloseIO    是否在内部关闭IO流
     * @return               返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToStream(String i_RowKey ,OutputStream io_Out ,boolean i_IsCloseIO) throws IOException
    {
        HBFile v_HBFile = this.readFileInfo(i_RowKey);
        
        return this.readToStream(v_HBFile ,io_Out ,i_IsCloseIO);
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入到IO流中。
     * 
     * 注：内部自动关闭IO流
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_HBFile       大文件信息对象
     * @param io_Out         IO流
     * @return               返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToStream(HBFile i_HBFile ,OutputStream io_Out) throws IOException
    {
        return this.readToStream(i_HBFile ,io_Out ,true);
    }
    
    
    
    /**
     * 从HBase数据库中将文件内容读取出来，同时写入到IO流中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @param i_HBFile       大文件信息对象
     * @param io_Out         IO流
     * @param i_IsCloseIO    是否在内部关闭IO流
     * @return               返回数据库中保存的文件基本信息
     * @throws IOException
     */
    public HBFile readToStream(HBFile i_HBFile ,OutputStream io_Out ,boolean i_IsCloseIO) throws IOException
    {
        if ( i_HBFile == null )
        {
            throw new NullPointerException("HBFile is null.");
        }
        
        if ( io_Out == null )
        {
            throw new NullPointerException("OutputStream is null.");
        }
        
        try
        {
            for (int i=1; i<=i_HBFile.getSegmentSize().intValue(); i++)
            {
                byte [] v_Data = this.read(i_HBFile.getId() ,i);
                // System.out.println("-- 读取分段[" + i + "]数据大小：" + StringHelp.getComputeUnit(v_Data.length));
                io_Out.write(v_Data);
            }
            
            io_Out.flush();
            return i_HBFile;
        }
        catch (Exception exce)
        {
            throw new IOException("readToStream is error. " + exce.getMessage());
        }
        finally
        {
            if ( i_IsCloseIO )
            {
                try
                {
                    io_Out.close();
                }
                catch (Exception exce)
                {
                    exce.printStackTrace();
                }
            }
        }
    }
    
    
    
    /**
     * 删除保存在HBase数据库中的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @param i_RowKey
     */
    public void delete(String i_RowKey)
    {
        HBFile v_HBFile = this.readFileInfo(i_RowKey);
        
        if ( v_HBFile == null )
        {
            return;
        }
        
        this.hbase.delete(this.bigFileInfo.getTableName() ,i_RowKey);
        for (int i=1; i<=v_HBFile.getSegmentSize().intValue(); i++)
        {
            this.hbase.delete(this.bigFileData.getTableName() ,i_RowKey + "_" + this.makeSegmentNo(i));
        }
    }
    
    
    
    /**
     * 获取：访问的HBase数据库
     */
    public HBase getHbase()
    {
        return hbase;
    }


    
    /**
     * 获取：大文件信息
     */
    public BigFileInfo getBigFileInfo()
    {
        return bigFileInfo;
    }


    
    /**
     * 获取：大文件数据
     */
    public BigFileData getBigFileData()
    {
        return bigFileData;
    }


    
    /**
     * 获取：分割序号的最大长度。默认为 6 位。这个属性并不实质性的影响任何操作。只影响序号前缀补0的个数
     */
    public int getSegmentMaxLen()
    {
        return segmentMaxLen;
    }


    
    /**
     * 设置：分割序号的最大长度。默认为 6 位。这个属性并不实质性的影响任何操作。只影响序号前缀补0的个数
     * 
     * @param segmentMaxLen 
     */
    public void setSegmentMaxLen(int segmentMaxLen)
    {
        this.segmentMaxLen = segmentMaxLen;
    }
    
}
