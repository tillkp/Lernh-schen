package de.lernhaeschen;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.*;
import java.util.*;
import java.text.*;

public class MainActivity extends Activity {
 final ArrayList<String[]> words=new ArrayList<>(),queue=new ArrayList<>();
 LinearLayout root; SharedPreferences prefs; TextToSpeech tts;
 String lang="Französisch"; int pos=0,correct=0,answered=0; boolean reverse=false;
 long sessionStart=0; String[] active; final Random rnd=new Random();
 final int PHOTO=123; final int PINK=Color.rgb(178,83,135);
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("bunny",0);load();ReminderReceiver.schedule(this);tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS)tts.setLanguage(Locale.FRANCE);});home();}
 String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date());}
 int mins(){return prefs.getInt("min-"+today(),0);}
 void time(){if(sessionStart==0)return;int m=(int)((System.currentTimeMillis()-sessionStart)/60000);if(m>0){prefs.edit().putInt("min-"+today(),mins()+m).apply();sessionStart=System.currentTimeMillis();}}
 void load(){words.clear();try(BufferedReader b=new BufferedReader(new InputStreamReader(getAssets().open("francais.txt"),"UTF-8"))){String s;while((s=b.readLine())!=null)addLine(s);}catch(Exception e){}for(String s:prefs.getString("custom","").split("\n"))addLine(s);}
 void addLine(String s){String[] a=s.split("\\|",3);if(a.length==3)words.add(a);}
 void page(String title){ScrollView s=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(255,249,252));root.setPadding(28,24,28,40);s.addView(root);setContentView(s);label(title,28);}
 void label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(65,55,72));t.setPadding(6,12,6,12);root.addView(t);}
 void btn(String name,Runnable action){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PINK));root.addView(b);b.setOnClickListener(v->action.run());}
 void home(){time();page("🐰 Lernhäschen");label("Dein kuscheliger Lernbuddy freut sich auf dich!",19);label("Heute: "+mins()+" / 15 Minuten · Erinnerung täglich 15:00 Uhr",16);label("💗 "+prefs.getInt("hearts",0)+" Herzen · 🪙 "+prefs.getInt("coins",0)+" Münzen",17);btn("🇫🇷 Französisch lernen",()->lessons("Französisch"));btn("📚 Latein lernen",()->lessons("Latein"));btn("📷 Vokabel-Foto importieren",this::photo);btn("🐰 Häschen und Zimmer",this::buddy);btn("🔔 Erinnerung testen",()->ReminderReceiver.notifyNow(this));}
 boolean isLang(String[] w,String l){return l.equals("Latein")?w[0].startsWith("Latein"):!w[0].startsWith("Latein");}
 void lessons(String l){lang=l;page(l+" – Lektion wählen");LinkedHashSet<String> units=new LinkedHashSet<>();for(String[] w:words)if(isLang(w,l))units.add(w[0]);if(units.isEmpty()){label("Noch keine Vokabeln. Importiere ein Foto!",17);btn("Foto wählen",this::photo);btn("Zurück",this::home);return;}
 Spinner spinner=new Spinner(this);ArrayList<String> opts=new ArrayList<>();opts.add("Alle Lektionen");opts.addAll(units);spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));root.addView(spinner);
 btn("Lernen starten",()->{queue.clear();String selected=(String)spinner.getSelectedItem();for(String[] w:words)if(isLang(w,l)&&(selected.equals("Alle Lektionen")||w[0].equals(selected)))queue.add(w);Collections.shuffle(queue);pos=0;answered=correct=0;sessionStart=System.currentTimeMillis();card();});btn("Zurück",this::home);}
 void card(){time();if(queue.isEmpty()){home();return;}if(pos>=queue.size())pos=0;active=queue.get(pos);reverse=rnd.nextBoolean();page("🐰 Vokabel "+(pos+1)+" / "+queue.size());label("Richtig: "+correct+" / "+answered,15);label(reverse?active[2]:active[1],26);if(!reverse&&lang.equals("Französisch"))btn("🔊 Anhören",()->{if(tts!=null)tts.speak(active[1],TextToSpeech.QUEUE_FLUSH,null,"word");});
 EditText input=new EditText(this);input.setHint(reverse?"Französisch eingeben":"Deutsch eingeben");root.addView(input);btn("Antwort prüfen",()->check(input.getText().toString()));btn("Lernen beenden",this::home);}
 String norm(String s){
  return java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT).trim(),java.text.Normalizer.Form.NFD)
    .replaceAll("\\p{M}","").replace("ß","ss")
    .replaceAll("[.!?]","").replaceAll("[’']","")
    .replaceAll("\\s*/\\s*","/").replaceAll("\\s*-\\s*","")
    .replaceAll("\\s+"," ").trim();
 }
 String germanEquivalent(String s){
  String n=norm(s).replaceFirst("^(ein|eine|einen|einem|einer|eines|der|die|das|den|dem|des)\\s+","");
  if(n.equals("opa")||n.equals("grossvater")||n.equals("grosspapa"))return "opa";
  if(n.equals("oma")||n.equals("grossmutter")||n.equals("grossmama"))return "oma";
  if(n.equals("mama")||n.equals("mutti")||n.equals("mutter"))return "mama";
  if(n.equals("papa")||n.equals("vati")||n.equals("vater"))return "papa";
  if(n.equals("fussball")||n.equals("fussballspielen"))return "fussball";
  return n;
 }
 boolean validAnswer(String expected,String answer,boolean french){
  String actual=norm(answer);
  if(actual.equals(norm(expected)))return true;
  for(String option:expected.split("[;/]")){
    String o=norm(option);
    if(o.equals(actual))return true;
    if(!french&&germanEquivalent(option).equals(germanEquivalent(answer)))return true;
  }
  return !french&&germanEquivalent(expected).equals(germanEquivalent(answer));
 }
 void check(String answer){
  if(answer.trim().isEmpty())return;
  String expected=reverse?active[1]:active[2];
  boolean isFrench=reverse&&lang.equals("Französisch");
  boolean ok=validAnswer(expected,answer,isFrench);
  answered++;
  if(ok){correct++;prefs.edit().putInt("hearts",prefs.getInt("hearts",0)+2).putInt("coins",prefs.getInt("coins",0)+1).apply();}
  else queue.add(active);
  page(ok?"🌟 Richtig!":"🌷 Noch nicht ganz");
  label("Richtige Antwort: "+expected,21);
  label("Deine Antwort: "+answer,17);
  if(ok&&isFrench&&!expected.equalsIgnoreCase(answer.trim()))
    label("Merke dir auch die Schreibweise mit Artikeln, Bindestrichen und Akzenten.",15);
  label(ok?"2 Herzen und 1 Münze für dein Häschen!":"Diese Vokabel kommt später noch mal.",17);
  btn("Weiter",()->{pos++;card();});
  btn("Beenden",this::home);
 }
 void buddy(){page("🐰 Mein Häschen");label("  (\\_/)\n  ( •ᴗ• ) ♥\n  / >🌷",29);label("💗 "+prefs.getInt("hearts",0)+" Herzen · 🪙 "+prefs.getInt("coins",0)+" Münzen",18);label("Dein Häschen bleibt immer glücklich, auch in Lernpausen.",16);
 String[] items={"🎀 Rosa Schleife","🌷 Blumentopf","⭐ Sternenlampe","☁️ Wolkenkissen"};int[] prices={10,20,25,30};for(int i=0;i<items.length;i++){final int index=i;boolean owned=prefs.getString("items","").contains(","+i+",");btn((owned?"✓ ":"🪙 "+prices[i]+" ")+items[i],()->{if(prefs.getString("items","").contains(","+index+","))return;int coins=prefs.getInt("coins",0);if(coins<prices[index]){Toast.makeText(this,"Noch nicht genug Münzen",Toast.LENGTH_SHORT).show();return;}prefs.edit().putInt("coins",coins-prices[index]).putString("items",prefs.getString("items","")+","+index+",").apply();buddy();});}btn("Zurück",this::home);}
 void photo(){Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(Intent.createChooser(i,"Vokabelseite auswählen"),PHOTO);}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=PHOTO||result!=RESULT_OK||data==null)return;try{InputImage image=InputImage.fromFilePath(this,data.getData());page("Foto wird gelesen …");TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).addOnSuccessListener(r->review(r.getText())).addOnFailureListener(e->{page("Texterkennung fehlgeschlagen");label(e.getMessage(),16);btn("Zurück",this::home);});}catch(Exception e){Toast.makeText(this,"Bild konnte nicht geöffnet werden",Toast.LENGTH_LONG).show();}}
 void review(String raw){page("📷 Vokabeln überprüfen");label("Die Texterkennung ist lokal. Bitte die passenden Wortpaare FRANZÖSISCH | DEUTSCH von der Seite eintragen oder korrigieren, bevor du speicherst.",16);
 Spinner unit=new Spinner(this);String[] options={"Au début","Unité 1","Unité 2","Unité 3","Latein: Eigene Lektion"};unit.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options));root.addView(unit);
 EditText edit=new EditText(this);edit.setMinLines(8);edit.setGravity(Gravity.TOP);edit.setHint("bonjour | Guten Tag!\nune rue | eine Straße");root.addView(edit);
 btn("Erkannten Text anzeigen",()->new AlertDialog.Builder(this).setTitle("Erkannter Text").setMessage(raw).setPositiveButton("OK",null).show());btn("Rohtext zum Bearbeiten übernehmen",()->edit.setText(raw));
 btn("Wortpaare speichern",()->{String u=(String)unit.getSelectedItem();StringBuilder b=new StringBuilder(prefs.getString("custom",""));int n=0;for(String line:edit.getText().toString().split("\n")){String[] a=line.split("\\|",2);if(a.length==2&&!a[0].trim().isEmpty()&&!a[1].trim().isEmpty()){b.append(u).append("|").append(a[0].trim()).append("|").append(a[1].trim()).append("\n");n++;}}if(n==0){Toast.makeText(this,"Erst Wortpaare mit | eintragen",Toast.LENGTH_LONG).show();return;}prefs.edit().putString("custom",b.toString()).apply();load();Toast.makeText(this,n+" Vokabeln gespeichert",Toast.LENGTH_LONG).show();home();});btn("Zurück",this::home);}
 @Override public void onBackPressed(){home();}
 @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}