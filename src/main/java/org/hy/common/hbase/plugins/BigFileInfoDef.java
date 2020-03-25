package org.hy.common.hbase.plugins;





/**
 * 大文件基本信息的的表信息的默认实现类 
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-03-14
 * @version     v1.0
 */
public class BigFileInfoDef implements BigFileInfo
{
    
    /** 文件信息表的名称 */
    private String tableName;
    
    /** 文件信息表的列簇名称 */
    private String familyName;
    
    /** 文件信息表的行主键的名称 */
    private String column_ID;
    
    /** 文件信息表中保存文件名称的字段名称 */
    private String column_FileName;
    
    /** 文件信息表中保存文件总大小的字段名称 */
    private String column_FileSize;
    
    /** 文件信息表中的文件类型的字段名称 */
    private String column_FileType;
    
    /** 文件信息表中保存文件被分割大小的字段名称 */
    private String column_SegmentSize;

    /** 文件信息表中文件保存时间的字段名称 */
    private String column_CreateTime;
    
    
    
    public BigFileInfoDef()
    {
        this("BigFileInfo" 
            ,"CF" 
            ,"id"
            ,"fileName"
            ,"fileSize"
            ,"fileType"
            ,"segmentSize"
            ,"createTime");
    }
    
    
    
    public BigFileInfoDef(String i_TableName ,String i_FamilyName)
    {
        this(i_TableName
            ,i_FamilyName
            ,"id"
            ,"fileName"
            ,"fileSize"
            ,"fileType"
            ,"segmentSize"
            ,"createTime");
    }
    
    
    
    public BigFileInfoDef(String i_TableName
                         ,String i_FamilyName
                         ,String i_Column_ID
                         ,String i_Column_FileName
                         ,String i_Column_FileSize
                         ,String i_Column_FileType
                         ,String i_Column_SegmentSize
                         ,String i_Column_CreateTime)
    {
        this.tableName          = i_TableName;
        this.familyName         = i_FamilyName;
        this.column_ID          = i_Column_ID;
        this.column_FileName    = i_Column_FileName;
        this.column_FileSize    = i_Column_FileSize;
        this.column_FileType    = i_Column_FileType;
        this.column_SegmentSize = i_Column_SegmentSize;
        this.column_CreateTime  = i_Column_CreateTime;
    }
    
    
    
    /**
     * 获取文件信息表的名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getTableName()
    {
        return this.tableName;
    }
    
    
    
    /**
     * 获取文件信息表的列簇名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getFamilyName()
    {
        return this.familyName;
    }
    
    
    
    /**
     * 获取文件信息表的行主键的名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_ID()
    {
        return this.column_ID;
    }
    
    
    
    /**
     * 获取文件信息表中保存文件名称的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_FileName()
    {
        return this.column_FileName;
    }
    
    
    
    /**
     * 获取文件信息表中保存文件总大小的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_FileSize()
    {
        return this.column_FileSize;
    }
    
    
    
    /**
     * 获取文件信息表中的文件类型的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_FileType()
    {
        return column_FileType;
    }
    
    
    
    /**
     * 获取文件信息表中保存文件被分割大小的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_SegmentSize()
    {
        return this.column_SegmentSize;
    }
    
    
    
    /**
     * 获取文件信息表中文件保存时间的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_CreateTime()
    {
        return column_CreateTime;
    }


    
    /**
     * 设置：文件信息表的名称
     * 
     * @param tableName 
     */
    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }


    
    /**
     * 设置：文件信息表的列簇名称
     * 
     * @param familyName 
     */
    public void setFamilyName(String familyName)
    {
        this.familyName = familyName;
    }


    
    /**
     * 设置：文件信息表的行主键的名称
     * 
     * @param column_ID 
     */
    public void setColumn_ID(String column_ID)
    {
        this.column_ID = column_ID;
    }


    
    /**
     * 设置：文件信息表中保存文件名称的字段名称
     * 
     * @param column_FileName 
     */
    public void setColumn_FileName(String column_FileName)
    {
        this.column_FileName = column_FileName;
    }


    
    /**
     * 设置：文件信息表中保存文件总大小的字段名称
     * 
     * @param column_FileSize 
     */
    public void setColumn_FileSize(String column_FileSize)
    {
        this.column_FileSize = column_FileSize;
    }



    /**
     * 设置：文件信息表中的文件类型的字段名称
     * 
     * @param column_FileType 
     */
    public void setColumn_FileType(String column_FileType)
    {
        this.column_FileType = column_FileType;
    }
    
    
    
    /**
     * 设置：文件信息表中保存文件被分割大小的字段名称
     * 
     * @param column_SegmentSize 
     */
    public void setColumn_SegmentSize(String column_SegmentSize)
    {
        this.column_SegmentSize = column_SegmentSize;
    }

    
    
    /**
     * 设置：文件信息表中文件保存时间的字段名称
     * 
     * @param column_CreateTime 
     */
    public void setColumn_CreateTime(String column_CreateTime)
    {
        this.column_CreateTime = column_CreateTime;
    }

}
