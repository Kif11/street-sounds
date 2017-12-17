//
//  SecondViewController.swift
//  qr-message
//
//  Created by Kirill Kovalewskiy  on 12/16/17.
//  Copyright © 2017 Kirill Kovalewskiy . All rights reserved.
//

import UIKit

class SecondViewController: UIViewController {
  
    let fileManager = FileManager.default

    override func viewDidLoad() {
      super.viewDidLoad()
      
      let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
      do {
        let fileURLs = try fileManager.contentsOfDirectory(at: documentsURL, includingPropertiesForKeys: nil)
        for url in fileURLs {
          print(url)
        }
      } catch {
        print(error)
      }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}
