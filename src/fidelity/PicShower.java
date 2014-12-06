
package fidelity;

import java.awt.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author A546456-Ananda Kumar Tokappa
 */

public class PicShower extends Canvas{

    private String image;
    Properties prop = new Properties();
    InputStream input;
    String nc[];

    public PicShower(String image){
        this.image=image;

        System.out.println("The matching Picyure is : "+image);
        try{
            input=new FileInputStream("nameCorpId.properties");
            prop.load(input);
            nc=prop.getProperty(image.substring(25).replaceFirst("\\d.jpg", "")).split(",");
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            try{
                input.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void paint(Graphics g){
        Toolkit t =Toolkit.getDefaultToolkit();
        Image i=t.getImage(image);
        g.drawImage(i, 0, 0,400,400, this);
        g.drawString("Name   : "+nc[0], 10, 20);
        g.drawString("CorpId : "+nc[1] , 10, 40);
    }
}
