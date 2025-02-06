## Dağıtık Sistem Programı 

Bu döküman, dağıtık sistem programının implementasyonunu, fonksiyonelliğini ve çalışma sürecini detaylandırmaktadır. Sistem, sunucu işlemleri için Java, gerçek zamanlı veri görselleştirmesi için Python ve sunucu yapılandırmalarını ve veri akışını yönetmek için Ruby kullanmaktadır. Program, hata toleranslı bir mimari kullanmakta, birden fazla istemciyi desteklemekte ve gerçek zamanlı veri senkronizasyonu sağlamaktadır.

---

### **Çalıştırma Süreci**

Programı çalıştırmak için aşağıdaki adımları izleyin:

1. **Plotting Sunucusunu Başlatın**
    - `plotter.py` dosyasını çalıştırarak Python tabanlı bir TCP sunucusu başlatın ve **7000** portunda dinleyin.
    - Bu sunucu, Admin ve Java sunucularından gelen kapasite verilerini görselleştirecektir.

2. **Java Sunucularını Başlatın**
    - IntelliJ IDEA veya benzer bir Java IDE kullanın.
    - Protobuf kütüphanesinin Maven ile doğru şekilde yapılandırıldığından emin olun.
    - Her bir sunucuyu (ör. `Server1.java`, `Server2.java` vb.) **Çalıştır > Hata Ayıklamayı Başlat (F5)** veya **Hata Ayıklamadan Çalıştır (Ctrl + F5)** ile başlatın.

3. **Admin İstemcisini Çalıştırın**
    - `admin.rb` dosyasını çalıştırın.
    - Admin, Plotter (7000 portu) ve Java sunucularına (5000, 5001, 5002 portları) bağlanmaya çalışır. Bağlantı hatası alırsanız, başarılı olana kadar tekrar eder.

4. **Java İstemcilerini Çalıştırın**
    - `Client.java` dosyasını kullanarak istemci talepleri gönderin.
---

### **Program İşleyişi**

#### **Admin Operasyonları**
- Admin (`admin.rb`), şu sunuculara bağlanır:
    1. **Plotter.py** kapasite görselleştirmesi için.
    2. **Java Sunucuları** yapılandırmaları göndermek ve kapasite verisi almak için.

- Bağlantı başarıyla sağlandıktan sonra:
    - Admin, Java sunucularına **hata toleransı seviyesi** ve bir **başlatma komutu** gönderir.
    - Sunucular, "YEP" veya "NOP" olarak yanıt verirler.

- Admin, **kapasite verisini** Java sunucularından talep eder ve bu veriyi Plotter'a iletir.

---

#### **Sunucu Operasyonları**
- Java sunucuları istemci taleplerini işler ve verileri diğer sunucularla senkronize eder.
- Temel özellikler:
    - **Hata Toleransı**:
        - Seviye 0: Hata toleransı yoktur.
        - Seviye 1 ve 2: Bir veya iki sunucu hatasına rağmen çalışabilir.
    - **Yedekleme ve Senkronizasyon**:
        - Sunucular, istemci verilerini birbirlerine yedekler.
        - Aynı anda gelen talepler çoklu iş parçacığı (multithreading) ile işlenir.

- **Talepleri İşleme**:
    - İstemciler, "SUB"(Abone ol), "DEL"(Kayıt sil), "CHECK"(Güncel bilgini sunucudan al), "ONLN"(Durumunu online yap), "OFLN"(Durumunu offline yap) talepleri gönderir.
    - Sunucular, istemcinin daha önce kayıtlı olup olmadığını kontrol eder.
    - Talepler işlenir ve istemcilere geri bildirim yapılır.

---

#### **Plotting Operasyonları**
- `plotter.py` dosyası, gerçek zamanlı verilerle sunucu kapasitelerini görselleştirir.
- Fonksiyonellik:
    - **7000** portunda Admin'den kapasite verisi bekler.
    - Her bir sunucu için ayrı kuyruklar tutarak kapasite durumlarını takip eder.
    - Matplotlib kullanarak her sunucu için gerçek zamanlı ve renkli bir grafik gösterir.

---

### **Sistem Özellikleri**

#### **Sunucu Yetenekleri**
- Java sunucuları:
    - Admin taleplerine kapasite ve yapılandırma ile yanıt verir.
    - Hata toleranslı durumları işler.
    - İstemci taleplerini dağıtık düğümler arasında işler ve senkronize eder.

#### **Hata Toleransı**
- Sistem üç seviyede hata toleransı sağlar:
    - Seviye 0: Hata toleransı yok (sıkı senkronizasyon).
    - Seviye 1: Bir sunucu hatasına toleranslıdır.
    - Seviye 2: İki sunucu hatasına toleranslıdır.

---

### **Yaygın Sorunlar ve Çözümleri**

#### **Çalıştırmadan Önce**
- Gerekli Python, Ruby ve Java kütüphanelerinin kurulu olduğundan emin olun.
- Java için, Protobuf kütüphanesi Maven aracılığıyla kurulmuştur. Bu yüzden maven ile bir "compile" işlemi yapmanız gerekebilir.

#### **Java Protobuf Hata Ayıklama**
- Maven bağımlılıkları arasında Protobuf kütüphanesinin bulunduğundan emin olun. `pom.xml` dosyasının doğru şekilde yapılandırıldığından emin olun.

---

### **Kurulum Kılavuzu**

#### **Java Sunucuları için**
1. Depoyu klonlayın.
2. IntelliJ IDEA'da Maven kurulumunu yapın.
3. IDE'yi kullanarak sunucuları çalıştırın (F5 veya Ctrl + F5).

#### **Python Plotter için**
1. Python 3 ve gerekli kütüphaneleri (protobuf, matplotlib) kurun.
2. `plotter.py` dosyasını bir Python yorumlayıcısı ile çalıştırın.

#### **Ruby Admin için**
1. Ruby ve gerekli gem'leri kurun.
2. Terminalde `admin.rb` dosyasını çalıştırın.

---

### **Sonuç**
Bu dağıtık sistem, etkili sunucu-istemci iletişimini, hata toleransını ve gerçek zamanlı veri görselleştirmesini göstermektedir. Modüler yapısı, esneklik ve ölçeklenebilirlik sağlar, bu da sistemi dağıtık talepleri işlemek için güçlü bir çözüm haline getirir.

## Ekip Üyeleri
- Muhammed Emin Çelik (22060398)
- Ali Kılıç(22060325)
- Arda Lafcı(22060320)

## Görev Dağılımı
- Muhammed Emin Çelik: Sunucu dizaynı
- Arda Lafcı: Admin ,Admin Handler ve Client Tasarımı  
- Ali Kılıç: Plotter, Proto ve Maven kurulumu 

## Github Reposu Açıklama 
Github reposunda dosya düzeni sıkıntısı çıktığı için force push yapıldığından commit geçmişleri silinmiştir(Git hatasından dolayı git dosyaları silindi. Fork commitleri de dahil)

## Youtube Videosu Linki
https://youtu.be/Wo12YIG_vFM?feature=shared