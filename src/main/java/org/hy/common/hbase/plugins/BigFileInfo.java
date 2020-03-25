package org.hy.common.hbase.plugins;





/**
 * 大文件基本的表信息 
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-03-14
 * @version     v1.0
 */
public interface BigFileInfo
{
    
    /**
     * 获取文件信息表的名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getTableName();
    
    
    
    /**
     * 获取文件信息表的列簇名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getFamilyName();
    
    
    
    /**
     * 获取文件信息表的行主键的名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-15
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_ID();
    
    
    
    /**
     * 获取文件信息表中保存文件名称的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_FileName();
    
    
    
    /**
     * 获取文件信息表中保存文件总大小的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_FileSize();
    
    
    
    /**
     * 获取文件信息表中的文件类型的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_FileType();
    
    
    
    /**
     * 获取文件信息表中保存文件被分割大小的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-14
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_SegmentSize();
    
    
    
    /**
     * 获取文件信息表中文件保存时间的字段名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-16
     * @version     v1.0
     *
     * @return
     */
    public String getColumn_CreateTime();
    
}
