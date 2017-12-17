#include "../include/base_64.h"
#include "../include/qr_chunk.hpp"

#include <iostream>
#include <fstream>
#include <math.h>
#include <string.h>


QrChunk::QrChunk(std::string data) {
  m_header = data.substr(0, m_header_size);
  m_body = data.substr(m_header_size, data.size() - m_header_size);
}

QrChunk::QrChunk(cv::Mat image) {
  m_image = image;
}

std::string QrChunk::getHeader() { return m_header; }

std::string QrChunk::getBody() { return m_body; }

std::string QrChunk::getData() { return m_header + m_body; }

int QrChunk::getCount () const {
  return this->count;
}

int QrChunk::getIndex () const {
  return this->index;
}

void QrChunk::setBody(std::string data) { m_body = data; }

void QrChunk::setImage(cv::Mat image) { m_image = image; }

bool QrChunk::operator<( const QrChunk& val ) const {
  return (this->getIndex() < val.getIndex());
}

bool QrChunk::setData ( std::string _message ) {
  m_header = _message.substr(0, m_header_size);
  m_body =  _message.substr(m_header_size, message.size() - m_header_size);
  
  try {
      this->index = std::stoi(base64_decode(m_header).substr(0,2));
      this->count = std::stoi(base64_decode(m_header).substr(2,4));
      this->message = _message;
      return true;
        
  } catch (int e) {
      std::cout << "[-] Header cannot be decoded" << std::endl;
      return false;
  }
}
