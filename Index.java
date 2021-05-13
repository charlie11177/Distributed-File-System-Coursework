import java.util.ArrayList;

public class Index {
    class FileInfo {
        public String name;
        public int filesize;
        public String status;
        public int[] storePorts;

        public FileInfo(String name, int filesize, String status, int[] storePorts){
            this.name = name;
            this.filesize = filesize;
            this.status = status;
            this.storePorts = storePorts;
        }
    }

    private ArrayList<FileInfo> index;
    
    public Index(){
        index = new ArrayList<FileInfo>();
    }

    public boolean add(String name, int filesize, String status, int[] storePorts){
        if(getFileInfo(name) == null){
            index.add(new FileInfo(name, filesize, status, storePorts));
            return true;
        }else{
            return false;
        }
    }

    public void remove(String name){
        index.remove(getFileInfo(name));
    }

    public String getFileList(){
        String file_list = "";
        for(FileInfo fInfo : index){
            if(fInfo.status == "store complete"){
                file_list += fInfo.name + " ";
            }
        }
        return file_list.trim();
    }

    public void changeStatus(String name, String status){
        getFileInfo(name).status = status;
    }

    public FileInfo getFileInfo(String name){
        for(FileInfo fileInfo : index){
            if(fileInfo.name.equals(name)){
                return fileInfo;
            }
        }
        return null;
    }
    
    public boolean fileExists(String name){
        return (getFileInfo(name) != null);
    }
}