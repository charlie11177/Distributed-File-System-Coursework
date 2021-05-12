import java.util.ArrayList;

public class Index {
    class FileInfo {
        public String name;
        public String status;

        public FileInfo(String name, String status){
            this.name = name;
            this.status = status;
        }
    }

    private ArrayList<FileInfo> index;
    
    public Index(){
        index = new ArrayList<FileInfo>();
    }

    public boolean add(String name, String status){
        if(getFileInfo(name) == null){
            index.add(new FileInfo(name, status));
            return true;
        }else{
            return false;
        }
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
            if(fileInfo.name == name){
                return fileInfo;
            }
        }
        return null;
    }
    
    public boolean fileExists(String name){
        return (getFileInfo(name) != null);
    }
}