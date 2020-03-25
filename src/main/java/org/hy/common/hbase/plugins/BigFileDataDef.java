package org.hy.common.hbase.plugins;





/**
 * 大文件数据的表信息(文件数据会被分割为多个小段数据保存)的默认实现类
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-03-14
 * @version     v1.0
 */
public class BigFileDataDef implements BigFileData
{
    
    /** 文件数据表的名称 */
    private String tableName;
    
    /** 文件数据表的列簇名称 */
    private String familyName;
    
    /** 文件数据表的数据块的大小(单位：KB) */
    private int    familyBlockSize;
    
    
    
    public BigFileDataDef()
    {
        this("BigFileData" ,"CF" ,512);  // 512KB
    }
    
    
    
    public BigFileDataDef(String i_TableName ,String i_FamilyName ,int i_FamilyBlockSize)
    {
        this.tableName       = i_TableName;
        this.familyName      = i_FamilyName;
        this.familyBlockSize = i_FamilyBlockSize;
    }
    
    
    
    /**
     * 获取文件数据表的名称
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
     * 获取文件数据表的列簇名称
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
     * 获取文件数据表的数据块的大小(单位：KB)
     * 
     * HBase存在数据块限制，需要根据应用进行调整。
     * 默认情况下，数据块限制为64KB。
     * 由于图片内容作为单元格(Cell)的值保存，其大小受制于数据块的大小。
     * 在应用中需根据最大图片大小对HBase数据块大小进行修改。
     * 用HColumnDescriptor.setBlocksize指定数据块大小，可分列簇指定。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public int getFamilyBlockSize()
    {
        return this.familyBlockSize;
    }

    
    
    /**
     * 设置：文件数据表的名称
     * 
     * @param tableName 
     */
    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }


    
    /**
     * 设置：文件数据表的列簇名称
     * 
     * @param familyName 
     */
    public void setFamilyName(String familyName)
    {
        this.familyName = familyName;
    }


    
    /**
     * 设置：文件数据表的数据块的大小(单位：KB)
     * 
     * @param familyBlockSize 
     */
    public void setFamilyBlockSize(int familyBlockSize)
    {
        this.familyBlockSize = familyBlockSize;
    }
    
}
