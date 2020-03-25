package org.hy.common.hbase;

import org.apache.hadoop.hbase.util.Bytes;

import org.hy.common.Help;
import org.hy.common.ListMap;





/**
 * HBase数据库的查询分页信息
 * 
 * @author ZhengWei(HY)
 * @create 2014-05-22
 */
public class HPage
{
    
    private long                      pageSize;
    
    /** 当前分页数据的最后一条记录的RowKey */
    private byte []                   rowKey;
    
    /** 历史分页数据的最后一条记录的RowKey的集合 */
    private ListMap<byte [] ,byte []> pageRowKeys;
    
    
    
    public HPage(long i_PageSize)
    {
        if ( i_PageSize < 1 )
        {
            throw new java.lang.InstantiationError("PageSize(" + i_PageSize + ") is not >= 1.");
        }
        
        this.pageSize    = i_PageSize;
        this.rowKey      = null;
        this.pageRowKeys = new ListMap<byte [] ,byte []>();
    }

    
    
    public HPage(long i_PageSize ,byte [] i_RowKey)
    {
        this(i_PageSize);
        this.setRowKey(i_RowKey);
    }
    
    
    
    public HPage(long i_PageSize ,String i_RowKey)
    {
        this(i_PageSize);
        this.setRowKey(i_RowKey);
    }
    
    
    
    @Override
    protected void finalize() throws Throwable
    {
        this.pageRowKeys = null;
        super.finalize();
    }



    public long getPageSize()
    {
        return pageSize;
    }

    
    
    public byte [] getRowKey()
    {
        return rowKey;
    }
    
    
    
    /**
     * 获取历史分页数据中某一页的最后一条记录的RowKey
     * 
     * @param i_PageIndex  分页的页编号。最小下标从 1 开始。
     * @return
     */
    public byte [] getRowKey(int i_PageIndex)
    {
        if ( i_PageIndex < 0 || this.pageRowKeys.size() > i_PageIndex )
        {
            throw new ArrayIndexOutOfBoundsException("PageIndex is 1 to " + this.pageRowKeys.size() + ".");
        }
        
        return this.pageRowKeys.get(i_PageIndex - 1);
    }
    
    
    
    public String getRowKeyString()
    {
        if ( this.rowKey != null )
        {
            return Bytes.toString(this.rowKey);
        }
        else
        {
            return null;
        }
    }
    
    
    
    /**
     * 获取历史分页数据中某一页的最后一条记录的RowKey
     * 
     * @param i_PageIndex  分页的页编号。最小下标从 1 开始。
     * @return
     */
    public String getRowKeyString(int i_PageIndex)
    {
        return Bytes.toString(this.getRowKey(i_PageIndex));
    }
    
    
    
    public byte [] getStartRowKey()
    {
        if ( this.rowKey != null )
        {
            return Bytes.add(this.rowKey ,new byte[]{ 0x00 });
        }
        else
        {
            return null;
        }
    }
    
    
    
    public void setRowKey(byte [] i_RowKey)
    {
        if ( i_RowKey == null || i_RowKey.length <= 0 )
        {
            this.rowKey = null;
        }
        else
        {
            this.rowKey = i_RowKey;
            
            if ( !this.pageRowKeys.containsKey(this.rowKey) )
            {
                this.pageRowKeys.put(this.rowKey ,this.rowKey);
            }
        }
    }
    
    
    
    public void setRowKey(String i_RowKey)
    {
        if ( !Help.isNull(i_RowKey) )
        {
            this.setRowKey(Bytes.toBytes(i_RowKey));
        }
        else
        {
            this.rowKey = null;
        }
    }
    
}
